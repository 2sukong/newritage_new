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
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnPrevMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1); loadData()
        }
        binding.btnNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1); loadData()
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
        }
    }

    private fun loadData() {
        val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentMonth.time)
        binding.tvMonthLabel.text = SimpleDateFormat("yyyy년 MM월", Locale.getDefault()).format(currentMonth.time)

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val sessions = db.sessionDao().getSessionsByMonth(yearMonth)

            if (sessions.isNotEmpty()) {
                binding.tvNoData.visibility = View.GONE
                binding.layoutStats.visibility = View.VISIBLE

                val avgPressure = sessions.map { it.avgPressure }.average().toFloat()
                val maxPressure = sessions.maxOf { it.maxPressure }
                val totalTime = sessions.sumOf { it.durationSeconds }
                val count = sessions.size

                binding.tvAvg.text = String.format("%.1f", avgPressure)
                binding.tvMax.text = String.format("%.1f", maxPressure)
                val min = totalTime / 60; val sec = totalTime % 60
                binding.tvMedTime.text = String.format("%02d:%02d", min, sec)
                binding.tvCount.text = "${count}회"

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
                binding.tvNoData.visibility = View.VISIBLE
                binding.layoutStats.visibility = View.GONE
                binding.lineChart.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
