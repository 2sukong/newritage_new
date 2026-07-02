package com.newritage.app.ui.main.analysis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.databinding.FragmentDailyAnalysisBinding
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
        binding.lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = Color.parseColor("#5A6B5A")
            // 일간 분석용 X축 가이드 (0분, 5분, 10분 형태로 나오도록 격자 조정)
            xAxis.setDrawGridLines(false)
            axisLeft.textColor = Color.parseColor("#5A6B5A")
            axisRight.isEnabled = false
            setGridBackgroundColor(Color.TRANSPARENT)
        }
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
                // [수정] 데이터가 있을 때 기존에 없던 형식을 XML ID 규칙에 맞게 매핑
                val min = session.durationSeconds / 60
                val sec = session.durationSeconds % 60

                // XML의 정식 ID들로 데이터 텍스트 바인딩
                binding.tvAvgPressure1.text = String.format("%.1f", session.avgPressure)
                binding.tvAvgPressure2.text = String.format("%.1f", session.minPressure) // 만약 DB에 minPressure가 없다면 적절한 변수나 session.avgPressure - 5f 등으로 대체 가능
                binding.tvAvgPressure3.text = String.format("%.1f", session.maxPressure)
                binding.tvMedTime.text = String.format("%02d:%02d", min, sec)

                // 센서별 상세 분석 텍스트 세팅 (임시 수치 대입 또는 DB 값 매핑)
                binding.tvSensorADetail.text = "최고 45.0 / 최저 10.2 / 평균 ${String.format("%.1f", session.avgPressure)} / 중앙 28.4 kPa"
                binding.tvSensorBDetail.text = "최고 42.1 / 최저 15.4 / 평균 ${String.format("%.1f", session.avgPressure - 1)} / 중앙 32.0 kPa"
                binding.tvSensorCDetail.text = "최고 51.3 / 최저 8.9 / 평균 ${String.format("%.1f", session.avgPressure + 2)} / 중앙 33.2 kPa"

                binding.tvDailyComment.text = "오늘의 명상 상태는 대체로 안정적입니다. 센서 A 구역의 압력이 다소 높게 유지되었으니 다음 명상 시 참고해 주세요."

                // 시뮬레이션 그래프 데이터 (실제 기기 데이터 없으므로 평균 기반 생성)
                val entries = generateSimulatedEntries(session.avgPressure, session.durationSeconds / 10)
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
                // [수정] 데이터가 없을 때 빈 값 처리 (--.-)
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

    private fun generateSimulatedEntries(avg: Float, count: Int): List<Entry> {
        val result = mutableListOf<Entry>()
        val n = maxOf(count, 10)
        var current = avg
        for (i in 0 until n) {
            val noise = (Math.random() - 0.5).toFloat() * 8f
            current = (current + noise).coerceIn(5f, 80f)
            result.add(Entry(i.toFloat(), current))
        }
        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}