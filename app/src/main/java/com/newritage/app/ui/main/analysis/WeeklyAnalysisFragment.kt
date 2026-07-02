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
import com.newritage.app.data.AppDatabase
import com.newritage.app.databinding.FragmentWeeklyAnalysisBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeeklyAnalysisFragment : Fragment() {

    private var _binding: FragmentWeeklyAnalysisBinding? = null
    private val binding get() = _binding!!
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

        // XML 파일의 화살표 ID가 주간용 명칭인 btnPrevWeek / btnNextWeek 인지 체크하세요!
        binding.btnPrevWeek.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
            loadData()
        }
        binding.btnNextWeek.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1)
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
            xAxis.setDrawGridLines(false) // 격자 제거로 일관된 디자인 유지
            axisLeft.textColor = Color.parseColor("#5A6B5A")
            axisRight.isEnabled = false
        }
    }

    private fun loadData() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val weekEnd = currentWeekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_WEEK, 6)

        val startStr = sdf.format(currentWeekStart.time)
        val endStr = sdf.format(weekEnd.time)

        // [수정] XML 날짜 라벨 ID 규칙인 tvDateLabel로 명칭 변경
        binding.tvDateLabel.text = "$startStr ~ $endStr"

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val sessions = db.sessionDao().getSessionsInRange(startStr, endStr)

            if (sessions.isNotEmpty()) {
                // [수정] 데이터가 존재할 때 수치 연산
                val avgPressure = sessions.map { it.avgPressure }.average().toFloat()
                val maxPressure = sessions.maxOf { it.maxPressure }
                val minPressure = sessions.minOf { it.minPressure } // 주간 최저 압력 연산 추가
                val totalTime = sessions.sumOf { it.durationSeconds }
                val count = sessions.size

                // [수정] 상단 5개 칸 공통 ID 매핑 규칙 적용
                binding.tvAvgPressure1.text = String.format("%.1f", avgPressure) // 주간 평균
                binding.tvAvgPressure2.text = String.format("%.1f", minPressure) // 주간 최저
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
                // [수정] 데이터가 없는 기간일 때 크래시 방지 및 안전 처리
                binding.tvAvgPressure1.text = "--.-"
                binding.tvAvgPressure2.text = "--.-"
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