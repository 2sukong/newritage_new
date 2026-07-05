package com.newritage.app.ui.main.analysis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.Session
import com.newritage.app.databinding.FragmentDailyAnalysisBinding
import com.newritage.app.ui.util.applyAnalysisStyle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailyAnalysisFragment : Fragment() {

    private var _binding: FragmentDailyAnalysisBinding? = null
    private val binding get() = _binding!!
    private var currentDate = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDailyAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnPrevDay.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            loadData()
        }
        binding.btnNextDay.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, 1)
            loadData()
        }

        setupChart()
        loadData()
    }

    private fun setupChart() {
        binding.lineChart.applyAnalysisStyle()
    }

    private fun loadData() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(currentDate.time)
        val displayStr = SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREAN).format(currentDate.time)
        binding.tvDateLabel.text = displayStr

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val session = db.sessionDao().getLatestSessionByDate(dateStr)

            if (session != null) {
                val min = session.durationSeconds / 60
                val sec = session.durationSeconds % 60

                binding.tvAvgPressure1.text = String.format("%.1f", session.avgPressure)
                binding.tvAvgPressure2.text = String.format("%.1f", session.minPressure)
                binding.tvAvgPressure3.text = String.format("%.1f", session.maxPressure)
                binding.tvMedTime.text = String.format("%02d:%02d", min, sec)

                // 부위별(엄지/검지·중지/손바닥) 상세 통계 — Session에 저장된 실측값
                binding.tvSensorADetail.text = String.format(
                    "최고 %.1f / 최저 %.1f / 평균 %.1f / 중앙 %.1f kPa",
                    session.thumbMax, session.thumbMin, session.thumbAvg, session.thumbMedian
                )
                binding.tvSensorBDetail.text = String.format(
                    "최고 %.1f / 최저 %.1f / 평균 %.1f / 중앙 %.1f kPa",
                    session.imMax, session.imMin, session.imAvg, session.imMedian
                )
                binding.tvSensorCDetail.text = String.format(
                    "최고 %.1f / 최저 %.1f / 평균 %.1f / 중앙 %.1f kPa",
                    session.palmMax, session.palmMin, session.palmAvg, session.palmMedian
                )

                binding.tvDailyComment.text = buildDailyComment(session)

                val readings = db.sessionDao().getReadingsByDate(dateStr)
                val entries = readings.mapIndexed { index, reading -> Entry(index.toFloat(), reading.overall) }
                val dataSet = LineDataSet(entries, "압력").apply {
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
                binding.tvAvgPressure2.text = "--.-"
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

    private fun buildDailyComment(session: Session): String {
        val regions = listOf(
            "엄지" to session.thumbAvg,
            "검지·중지" to session.imAvg,
            "손바닥" to session.palmAvg
        )
        val highest = regions.maxByOrNull { it.second }
        val overall = if (session.avgPressure < 35) "대체로 안정적입니다" else "다소 긴장된 편이었습니다"
        return if (highest != null && highest.second > 0f) {
            "오늘의 명상 상태는 $overall. ${highest.first} 부위의 압력이 다소 높게 유지되었으니 다음 명상 시 참고해 주세요."
        } else {
            "오늘의 명상 상태는 $overall."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}