package com.newritage.app.ui.measurement

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.newritage.app.R
import com.newritage.app.databinding.ActivityMeasurementBinding
import kotlin.random.Random

class MeasurementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeasurementBinding

    private val handler = Handler(Looper.getMainLooper())
    private var measuring = false
    private var elapsedSeconds = 0
    private val pressureReadings = mutableListOf<Float>()
    private val chartEntries = mutableListOf<Entry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeasurementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupChart()
        showGuideDialog()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnStop.setOnClickListener { stopMeasurement() }
    }

    private fun showGuideDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_measurement_guide, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<View>(R.id.btnStartGuide).setOnClickListener {
            dialog.dismiss()
            startMeasurement()
        }
        dialog.show()
    }

    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setBackgroundColor(Color.TRANSPARENT)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = Color.parseColor("#5A6B5A")
            xAxis.setDrawGridLines(false)
            axisLeft.textColor = Color.parseColor("#5A6B5A")
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 80f
            axisRight.isEnabled = false
        }
    }

    private fun startMeasurement() {
        measuring = true
        elapsedSeconds = 0
        pressureReadings.clear()
        chartEntries.clear()
        binding.btnStop.isEnabled = true
        measureLoop.run()
    }

    private val measureLoop = object : Runnable {
        override fun run() {
            if (!measuring) return
            elapsedSeconds++

            // 시뮬레이션 압력 값 생성
            val prev = pressureReadings.lastOrNull() ?: 30f
            val noise = (Random.nextFloat() - 0.5f) * 10f
            val pressure = (prev + noise).coerceIn(5f, 75f)
            pressureReadings.add(pressure)

            // UI 업데이트
            val min = elapsedSeconds / 60
            val sec = elapsedSeconds % 60
            binding.tvTimer.text = String.format("%02d:%02d", min, sec)
            binding.tvCurrentPressure.text = String.format("%.1f", pressure)

            // 차트 업데이트
            chartEntries.add(Entry(elapsedSeconds.toFloat(), pressure))
            updateChart()

            handler.postDelayed(this, 1000L)
        }
    }

    private fun updateChart() {
        val visibleEntries = if (chartEntries.size > 60) {
            chartEntries.takeLast(60)
        } else {
            chartEntries.toList()
        }

        val dataSet = LineDataSet(visibleEntries, "압력").apply {
            color = Color.parseColor("#8B9E7B")
            setDrawCircles(false)
            lineWidth = 2f
            setDrawFilled(true)
            fillColor = Color.parseColor("#8B9E7B")
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.notifyDataSetChanged()
        binding.lineChart.invalidate()
    }

    private fun stopMeasurement() {
        measuring = false
        handler.removeCallbacks(measureLoop)

        if (pressureReadings.isEmpty()) {
            finish()
            return
        }

        val avgPressure = pressureReadings.average().toFloat()
        val maxPressure = pressureReadings.max()
        val minPressure = pressureReadings.min()

        val intent = Intent(this, SessionCompleteActivity::class.java).apply {
            putExtra("duration_seconds", elapsedSeconds)
            putExtra("avg_pressure", avgPressure)
            putExtra("max_pressure", maxPressure)
            putExtra("min_pressure", minPressure)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        measuring = false
        handler.removeCallbacks(measureLoop)
    }
}
