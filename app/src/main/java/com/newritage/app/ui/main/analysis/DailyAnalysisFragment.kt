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
                binding.tvNoData.visibility = View.GONE
                binding.layoutStats.visibility = View.VISIBLE

                val min = session.durationSeconds / 60
                val sec = session.durationSeconds % 60
                binding.tvAvg.text = String.format("%.1f", session.avgPressure)
                binding.tvMax.text = String.format("%.1f", session.maxPressure)
                binding.tvMedTime.text = String.format("%02d:%02d", min, sec)

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
                binding.tvNoData.visibility = View.VISIBLE
                binding.layoutStats.visibility = View.GONE
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
