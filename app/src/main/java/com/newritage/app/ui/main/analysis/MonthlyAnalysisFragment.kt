package com.newritage.app.ui.main.analysis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.GeminiRepository
import com.newritage.app.data.Session
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.FragmentMonthlyAnalysisBinding
import com.newritage.app.ui.main.MainActivity
import com.newritage.app.ui.main.analysis.model.ComparisonSummary
import com.newritage.app.ui.main.analysis.model.DayIndicatorStatus
import com.newritage.app.ui.util.applyAnalysisStyle
import com.newritage.app.ui.util.setNavArrowEnabled
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MonthlyAnalysisFragment : Fragment() {

    private var _binding: FragmentMonthlyAnalysisBinding? = null
    private val binding get() = _binding!!
    private var currentMonth = Calendar.getInstance()
    private val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthlyAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // ±1달 대신, 측정 기록이 있는 가장 가까운 달로 건너뛴다(측정 안 한 달은 건너뜀).
        binding.btnPrevMonth.setOnClickListener { navigateToMeasured(-1) }
        binding.btnNextMonth.setOnClickListener { navigateToMeasured(+1) }
        binding.emptyStartMeasure.setOnClickListener { (activity as? MainActivity)?.switchToHomeTab() }

        setupChart()
        // 최초 진입 시 가장 최근 측정일이 속한 달로 맞춘다.
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).sessionDao()
            dao.getLatestSessionDate()?.let { latest ->
                dateSdf.parse(latest)?.let { currentMonth.time = it }
            }
            loadData()
        }
    }

    /** direction<0: 이전 측정 달, direction>0: 다음 측정 달로 이동. 없으면 토스트 후 그대로. */
    private fun navigateToMeasured(direction: Int) {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).sessionDao()
            val ym = monthSdf.format(currentMonth.time)
            // "yyyy-MM-01"보다 이전 / "yyyy-MM-31"보다 이후 → 문자열 비교로 다른 달의 가장 가까운 측정일.
            val target = if (direction < 0) {
                dao.getPrevSessionDate("$ym-01")
            } else {
                dao.getNextSessionDate("$ym-31")
            }
            if (target == null) {
                Toast.makeText(requireContext(), R.string.analysis_no_more_records, Toast.LENGTH_SHORT).show()
                return@launch
            }
            dateSdf.parse(target)?.let { currentMonth.time = it }
            loadData()
        }
    }

    private fun setupChart() {
        binding.lineChart.applyAnalysisStyle()
    }

    private fun loadData() {
        val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentMonth.time)
        binding.tvDateLabel.text = SimpleDateFormat("yyyy.MM", Locale.getDefault()).format(currentMonth.time)

        val previousMonth = currentMonth.clone() as Calendar
        previousMonth.add(Calendar.MONTH, -1)
        val previousYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(previousMonth.time)

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val dao = db.sessionDao()

            // 측정 기록이 하나도 없으면 분석 섹션을 모두 감추고 안내만 보여준다.
            if (dao.getLatestSessionDate() == null) {
                binding.contentSections.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                binding.btnPrevMonth.setNavArrowEnabled(false)
                binding.btnNextMonth.setNavArrowEnabled(false)
                return@launch
            }
            binding.contentSections.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE

            // 이동 가능한 방향의 화살표만 활성화한다(더 없으면 회색 비활성).
            binding.btnPrevMonth.setNavArrowEnabled(dao.getPrevSessionDate("$yearMonth-01") != null)
            binding.btnNextMonth.setNavArrowEnabled(dao.getNextSessionDate("$yearMonth-31") != null)

            val sessions = dao.getSessionsByMonth(yearMonth)
            val previousSessions = dao.getSessionsByMonth(previousYearMonth)

            binding.comparisonCard.bind(ComparisonSummary.from(sessions, previousSessions))
            val baselineOverall = UserPreferences(requireContext()).baselineOverall
            val indicators = buildDayIndicators(sessions, baselineOverall)
            binding.monthCalendar.bind(
                year = currentMonth.get(Calendar.YEAR),
                month = currentMonth.get(Calendar.MONTH) + 1,
                indicators = indicators
            )

            // AI 코멘트는 네트워크 호출이라 응답이 오래 걸릴 수 있으므로, 아래 통계/그래프 렌더링을
            // 막지 않도록 별도 코루틴에서 독립적으로 갱신한다(최근 30일 기준 Gemini API 우선, 실패 시 로컬 폴백).
            // 단, 마지막 생성 이후 새로 저장된 세션이 없으면 API를 다시 호출하지 않고 캐시된 코멘트를 그대로 보여준다.
            lifecycleScope.launch {
                val prefs = UserPreferences(requireContext())
                val latestSessionId = db.sessionDao().getLatestSessionId() ?: -1L
                val cachedComment = prefs.monthlyAiComment

                if (cachedComment.isNotEmpty() && prefs.monthlyAiCommentSessionId == latestSessionId) {
                    _binding?.aiCommentCard?.setComment(cachedComment)
                } else {
                    _binding?.aiCommentCard?.setComment(getString(R.string.ai_comment_loading))
                    // 해당 월의 마지막 날을 기준으로 AI 피드백을 요청한다.
                    val cal = currentMonth.clone() as Calendar
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    val endDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

                    val aiComment = GeminiRepository(db.sessionDao()).generateMonthlyFeedback(endDateStr)
                    if (aiComment != null) {
                        prefs.monthlyAiComment = aiComment
                        prefs.monthlyAiCommentSessionId = latestSessionId
                        _binding?.aiCommentCard?.setComment(aiComment)
                    } else {
                        _binding?.aiCommentCard?.setComment(MonthlyCommentGenerator.generate(sessions))
                    }
                }
            }

            if (sessions.isNotEmpty()) {
                val avgPressure = sessions.map { it.avgPressure }.average().toFloat()
                val maxPressure = sessions.maxOf { it.maxPressure }
                val totalTime = sessions.sumOf { it.durationSeconds }
                val count = sessions.size

                binding.tvAvgPressure1.text = String.format("%.1f", avgPressure) // 월간 평균
                binding.tvAvgPressure3.text = String.format("%.1f", maxPressure) // 월간 최고

                val min = totalTime / 60; val sec = totalTime % 60
                binding.tvMedTime.text = String.format("%02d:%02d", min, sec)   // 월간 총 명상 시간
                binding.tvMedCount.text = count.toString()                      // 월간 총 명상 횟수

                val entries = sessions.mapIndexed { i, s -> Entry(i.toFloat(), s.avgPressure) }
                val dataSet = LineDataSet(entries, "월간 압력").apply {
                    color = Color.parseColor("#8B9E7B")
                    setDrawCircles(false)
                    lineWidth = 2f
                    setDrawFilled(true)
                    fillColor = Color.parseColor("#8B9E7B")
                    fillAlpha = 40
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }
                binding.lineChart.data = LineData(dataSet)
                binding.lineChart.invalidate()
            } else {
                binding.tvAvgPressure1.text = "--.-"
                binding.tvAvgPressure3.text = "--.-"
                binding.tvMedTime.text = "--:--"
                binding.tvMedCount.text = "-"
                binding.lineChart.clear()
            }
        }
    }

    /**
     * 일자별 평균 압력을 baseline(긴장도 측정 페이지에서 저장한 기준값) 대비 변화율로 판정한다.
     * ThreadColors.assignColor와 동일한 절대 기준(±20%)을 사용하여
     * 한 달에 초록/빨강이 각각 하나씩만 나오는 상대 평가(최솟값/최댓값)를 피한다.
     */
    private fun buildDayIndicators(sessions: List<Session>, baselineOverall: Float): Map<Int, DayIndicatorStatus> {
        val avgPressureByDate = sessions.groupBy { it.date }
            .mapValues { (_, daySessions) -> daySessions.map { it.avgPressure }.average() }
        if (avgPressureByDate.isEmpty()) return emptyMap()

        val safeBaseline = baselineOverall.coerceAtLeast(1f)

        return avgPressureByDate.entries.associate { (date, avgPressure) ->
            val day = date.substringAfterLast("-").toIntOrNull() ?: 0
            val changeRate = ((avgPressure - safeBaseline) / safeBaseline) * 100f
            val status = when {
                changeRate >= 20f -> DayIndicatorStatus.TENSE
                changeRate <= -20f -> DayIndicatorStatus.STABLE
                else -> DayIndicatorStatus.NORMAL
            }
            day to status
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}