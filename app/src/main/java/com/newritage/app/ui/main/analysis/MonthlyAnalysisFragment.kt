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
import com.newritage.app.databinding.FragmentMonthlyAnalysisBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthlyAnalysisFragment : Fragment() {

    private var _binding: FragmentMonthlyAnalysisBinding? = null
    private val binding get() = _binding!!
    private var currentMonth = Calendar.getInstance()

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

        // 이전 달 버튼
        binding.btnPrevMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            loadData()
        }

        // 다음 달 버튼 (★ 괄호 짝 맞추기 수정)
        binding.btnNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            loadData()
        } // <- 여기서 중괄호와 소괄호가 정확히 닫혀야 합니다!

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
            xAxis.setDrawGridLines(false) // 격자 제거로 스크롤 레이아웃과 디자인 통일
            axisLeft.textColor = Color.parseColor("#5A6B5A")
            axisRight.isEnabled = false
        }
    }

    private fun loadData() {
        val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentMonth.time)
        // [수정] XML의 날짜 라벨 ID인 tvDateLabel로 명칭 변경 (2026.05 형식으로 깔끔하게 노출)
        binding.tvDateLabel.text = SimpleDateFormat("yyyy.MM", Locale.getDefault()).format(currentMonth.time)

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val sessions = db.sessionDao().getSessionsByMonth(yearMonth)

            if (sessions.isNotEmpty()) {
                // [수정] 한 달 동안 누적된 데이터를 조건대로 연산 가공
                val avgPressure = sessions.map { it.avgPressure }.average().toFloat()
                val maxPressure = sessions.maxOf { it.maxPressure }
                val minPressure = sessions.minOf { it.minPressure } // 월간 최저 압력 연산 추가
                val totalTime = sessions.sumOf { it.durationSeconds }
                val count = sessions.size

                // [수정] 월간 XML 구조와 완벽히 동일한 ID 규칙으로 그릇 채우기
                binding.tvAvgPressure1.text = String.format("%.1f", avgPressure) // 월간 평균
                binding.tvAvgPressure2.text = String.format("%.1f", maxPressure) // 월간 최고
                binding.tvAvgPressure3.text = String.format("%.1f", minPressure) // 월간 최저

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
                // [수정] 해당 월에 데이터가 전혀 없을 때 튕기거나 숨기지 않고 빈 값 방어 처리
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