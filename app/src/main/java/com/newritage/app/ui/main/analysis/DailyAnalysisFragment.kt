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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.SensorReading
import com.newritage.app.databinding.FragmentDailyAnalysisBinding
import com.newritage.app.ui.main.MainActivity
import com.newritage.app.ui.util.applyAnalysisStyle
import com.newritage.app.ui.util.setNavArrowEnabled
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailyAnalysisFragment : Fragment() {

    private var _binding: FragmentDailyAnalysisBinding? = null
    private val binding get() = _binding!!
    private var currentDate = Calendar.getInstance()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDailyAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // ±1일 대신, 측정 기록이 있는 가장 가까운 날짜로 건너뛴다(측정 안 한 날은 건너뜀).
        binding.btnPrevDay.setOnClickListener { navigateToMeasured(-1) }
        binding.btnNextDay.setOnClickListener { navigateToMeasured(+1) }
        binding.emptyStartMeasure.setOnClickListener { (activity as? MainActivity)?.switchToHomeTab() }

        setupChart()
        // 최초 진입 시 가장 최근 측정 날짜로 맞춘다(측정한 적 없으면 오늘 그대로).
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).sessionDao()
            dao.getLatestSessionDate()?.let { latest ->
                sdf.parse(latest)?.let { currentDate.time = it }
            }
            loadData()
        }
    }

    /** direction<0: 이전 측정일, direction>0: 다음 측정일로 이동. 없으면 토스트 후 그대로 둔다. */
    private fun navigateToMeasured(direction: Int) {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).sessionDao()
            val cur = sdf.format(currentDate.time)
            val target = if (direction < 0) dao.getPrevSessionDate(cur) else dao.getNextSessionDate(cur)
            if (target == null) {
                Toast.makeText(requireContext(), R.string.analysis_no_more_records, Toast.LENGTH_SHORT).show()
                return@launch
            }
            sdf.parse(target)?.let { currentDate.time = it }
            loadData()
        }
    }

    private fun setupChart() {
        binding.lineChart.applyAnalysisStyle()
    }

    private fun loadData() {
        val dateStr = sdf.format(currentDate.time)
        val displayStr = SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREAN).format(currentDate.time)
        binding.tvDateLabel.text = displayStr

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val dao = db.sessionDao()

            // 측정 기록이 하나도 없으면 분석 섹션을 모두 감추고 안내만 보여준다.
            if (dao.getLatestSessionDate() == null) {
                binding.contentSections.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                binding.btnPrevDay.setNavArrowEnabled(false)
                binding.btnNextDay.setNavArrowEnabled(false)
                return@launch
            }
            binding.contentSections.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE

            // 이동 가능한 방향의 화살표만 활성화한다(더 없으면 회색 비활성).
            binding.btnPrevDay.setNavArrowEnabled(dao.getPrevSessionDate(dateStr) != null)
            binding.btnNextDay.setNavArrowEnabled(dao.getNextSessionDate(dateStr) != null)

            val session = dao.getLatestSessionByDate(dateStr)

            if (session != null) {
                val min = session.durationSeconds / 60
                val sec = session.durationSeconds % 60

                binding.tvAvgPressure1.text = String.format("%.1f", session.avgPressure)
                binding.tvAvgPressure3.text = String.format("%.1f", session.maxPressure)
                binding.tvMedTime.text = String.format("%02d:%02d", min, sec)

                // 부위별(엄지/검지·중지/손바닥) 상세 통계 — Session에 저장된 실측값
                binding.tvSensorADetail.text = String.format(
                    "최고 %.1f / 평균 %.1f kPa",
                    session.thumbMax, session.thumbAvg
                )
                binding.tvSensorBDetail.text = String.format(
                    "최고 %.1f / 평균 %.1f kPa",
                    session.imMax, session.imAvg
                )
                binding.tvSensorCDetail.text = String.format(
                    "최고 %.1f / 평균 %.1f kPa",
                    session.palmMax, session.palmAvg
                )

                binding.tvDailyComment.text =
                    session.aiFeedback.ifBlank { "측정된 데이터가 없어 코멘트를 생성할 수 없습니다." }

                val readings = db.sessionDao().getReadingsByDate(dateStr)
                binding.lineChart.data = buildDailyPressureData(readings)
                binding.lineChart.invalidate()
            } else {
                binding.tvAvgPressure1.text = "--.-"
                binding.tvAvgPressure3.text = "--.-"
                binding.tvMedTime.text = "--:--"
                binding.tvSensorADetail.text = "데이터가 없습니다."
                binding.tvSensorBDetail.text = "데이터가 없습니다."
                binding.tvSensorCDetail.text = "데이터가 없습니다."
                binding.tvDailyComment.text = "측정된 데이터가 없어 코멘트를 생성할 수 없습니다."
                binding.lineChart.clear()
            }
        }
    }

    /**
     * 하루치 원시 데이터를 세션 단위로 나눠 세션마다 다른 색의 LineDataSet을 만든다.
     * x축 인덱스는 하루 전체에서 계속 이어지며(세션이 바뀌어도 0으로 리셋되지 않음),
     * 세션 경계에서는 이전 세션의 마지막 점을 다음 세션 데이터에 포함시켜 선이 끊기지 않게 연결한다.
     */
    private fun buildDailyPressureData(readings: List<SensorReading>): LineData {
        val sessionColors = listOf("#8B9E7B", "#6C8FB6", "#D1B06B", "#A67C8E", "#7B8FA6")
        val dataSets = mutableListOf<LineDataSet>()

        var index = 0
        var segmentStart = 0
        while (segmentStart < readings.size) {
            val sessionId = readings[segmentStart].sessionId
            var segmentEnd = segmentStart
            while (segmentEnd < readings.size && readings[segmentEnd].sessionId == sessionId) segmentEnd++

            val entries = mutableListOf<Entry>()
            if (dataSets.isNotEmpty()) {
                // 이전 세션의 마지막 점을 이어붙여 선이 끊기지 않도록 함
                entries.add(Entry((index - 1).toFloat(), readings[segmentStart - 1].overall))
            }
            for (i in segmentStart until segmentEnd) {
                entries.add(Entry(index.toFloat(), readings[i].overall))
                index++
            }

            val color = Color.parseColor(sessionColors[dataSets.size % sessionColors.size])
            dataSets.add(
                LineDataSet(entries, "세션 ${dataSets.size + 1}").apply {
                    this.color = color
                    setDrawCircles(false)
                    lineWidth = 2f
                    setDrawFilled(true)
                    fillColor = color
                    fillAlpha = 40
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }
            )

            segmentStart = segmentEnd
        }

        return LineData(dataSets as List<ILineDataSet>)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}