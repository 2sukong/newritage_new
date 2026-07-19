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
import com.newritage.app.databinding.FragmentWeeklyAnalysisBinding
import com.newritage.app.ui.main.MainActivity
import com.newritage.app.ui.main.analysis.model.ComparisonSummary
import com.newritage.app.ui.util.applyAnalysisStyle
import com.newritage.app.ui.util.setNavArrowEnabled
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeeklyAnalysisFragment : Fragment() {

    private var _binding: FragmentWeeklyAnalysisBinding? = null
    private val binding get() = _binding!!
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentWeekStart = Calendar.getInstance().also {
        it.set(Calendar.DAY_OF_WEEK, it.firstDayOfWeek)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeeklyAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // ±1주 대신, 측정 기록이 있는 가장 가까운 주로 건너뛴다(측정 안 한 주는 건너뜀).
        binding.btnPrevWeek.setOnClickListener { navigateToMeasured(-1) }
        binding.btnNextWeek.setOnClickListener { navigateToMeasured(+1) }
        binding.emptyStartMeasure.setOnClickListener { (activity as? MainActivity)?.switchToHomeTab() }
        setupChart()
        // 최초 진입 시 가장 최근 측정일이 속한 주로 맞춘다.
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).sessionDao()
            dao.getLatestSessionDate()?.let { currentWeekStart = weekStartOf(it) }
            loadData()
        }
    }

    /** direction<0: 이전 측정 주, direction>0: 다음 측정 주로 이동. 없으면 토스트 후 그대로. */
    private fun navigateToMeasured(direction: Int) {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).sessionDao()
            val weekEnd = (currentWeekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_WEEK, 6) }
            val target = if (direction < 0) {
                dao.getPrevSessionDate(sdf.format(currentWeekStart.time))
            } else {
                dao.getNextSessionDate(sdf.format(weekEnd.time))
            }
            if (target == null) {
                Toast.makeText(requireContext(), R.string.analysis_no_more_records, Toast.LENGTH_SHORT).show()
                return@launch
            }
            currentWeekStart = weekStartOf(target)
            loadData()
        }
    }

    /** "yyyy-MM-dd" 날짜가 속한 주의 첫 날(firstDayOfWeek)로 맞춘 Calendar를 만든다. */
    private fun weekStartOf(dateStr: String): Calendar = Calendar.getInstance().apply {
        sdf.parse(dateStr)?.let { time = it }
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    }

    private fun setupChart() {
        binding.lineChart.applyAnalysisStyle()
    }

    private fun loadData() {
        val weekEnd = currentWeekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_WEEK, 6)

        val startStr = sdf.format(currentWeekStart.time)
        val endStr = sdf.format(weekEnd.time)

        val previousWeekStart = currentWeekStart.clone() as Calendar
        previousWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
        val previousWeekEnd = previousWeekStart.clone() as Calendar
        previousWeekEnd.add(Calendar.DAY_OF_WEEK, 6)
        val prevStartStr = sdf.format(previousWeekStart.time)
        val prevEndStr = sdf.format(previousWeekEnd.time)

        binding.tvDateLabel.text = "$startStr ~ $endStr"

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val dao = db.sessionDao()

            // 측정 기록이 하나도 없으면 분석 섹션을 모두 감추고 안내만 보여준다.
            if (dao.getLatestSessionDate() == null) {
                binding.contentSections.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                binding.btnPrevWeek.setNavArrowEnabled(false)
                binding.btnNextWeek.setNavArrowEnabled(false)
                return@launch
            }
            binding.contentSections.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE

            // 이동 가능한 방향의 화살표만 활성화한다(더 없으면 회색 비활성).
            binding.btnPrevWeek.setNavArrowEnabled(dao.getPrevSessionDate(startStr) != null)
            binding.btnNextWeek.setNavArrowEnabled(dao.getNextSessionDate(endStr) != null)

            val sessions = dao.getSessionsInRange(startStr, endStr)
            val previousSessions = dao.getSessionsInRange(prevStartStr, prevEndStr)
            binding.comparisonCard.bind(ComparisonSummary.from(sessions, previousSessions))

            if (sessions.isNotEmpty()) {
                val avgPressure = sessions.map { it.avgPressure }.average().toFloat()
                val maxPressure = sessions.maxOf { it.maxPressure }
                val totalTime = sessions.sumOf { it.durationSeconds }
                val count = sessions.size

                binding.tvAvgPressure1.text = String.format("%.1f", avgPressure) // 주간 평균
                binding.tvAvgPressure3.text = String.format("%.1f", maxPressure) // 주간 최고

                val min = totalTime / 60; val sec = totalTime % 60
                binding.tvMedTime.text = String.format("%02d:%02d", min, sec)   // 주간 총 시간
                binding.tvMedCount.text = count.toString()                      // 주간 총 횟수

                // 일별 평균 그래프
                val entries = sessions.mapIndexed { i, s ->
                    Entry(i.toFloat(), s.avgPressure)
                }
                val dataSet = LineDataSet(entries, "주간 압력").apply {
                    color = Color.parseColor("#8B9E7B")
                    setDrawCircles(true)
                    circleRadius = 4f
                    setCircleColor(Color.parseColor("#8B9E7B"))
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}