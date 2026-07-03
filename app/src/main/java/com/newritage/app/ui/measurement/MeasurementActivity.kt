package com.newritage.app.ui.measurement

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.newritage.app.R
import com.newritage.app.ble.BleManager
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityMeasurementBinding
import com.newritage.app.ui.settings.VibrationPatterns

class MeasurementActivity : AppCompatActivity() {

    private enum class TensionState { CALM, TENSE }

    private lateinit var binding: ActivityMeasurementBinding
    private lateinit var prefs: UserPreferences

    private val handler = Handler(Looper.getMainLooper())
    private var measuring = false
    private var elapsedSeconds = 0
    private val pressureReadings = mutableListOf<Float>()
    private val chartEntries = mutableListOf<Entry>()

    // BLE SENSOR 특성에서 마지막으로 받은 total(f0+f1+f2) 값
    private var latestTotal = 0
    private var baselineTotal = 0
    private var tensionState = TensionState.CALM

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) {
                BleManager.startScan(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeasurementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)
        baselineTotal = prefs.baselinePressure.toInt()

        setupChart()
        showGuideDialog()
        connectBle()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnStop.setOnClickListener { stopMeasurement() }
    }

    private fun connectBle() {
        if (BleManager.hasRequiredPermissions(this)) {
            BleManager.startScan(this)
        } else {
            permissionLauncher.launch(BleManager.requiredPermissions())
        }
    }

    override fun onStart() {
        super.onStart()
        BleManager.onSensorData = { _, _, _, total -> latestTotal = total }
    }

    override fun onStop() {
        super.onStop()
        BleManager.onSensorData = null
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
            axisLeft.axisMaximum = 12285f // f0+f1+f2 이론상 최댓값(4095*3)
            axisRight.isEnabled = false
        }
    }

    private fun startMeasurement() {
        measuring = true
        elapsedSeconds = 0
        pressureReadings.clear()
        chartEntries.clear()
        tensionState = TensionState.CALM
        binding.btnStop.isEnabled = true
        measureLoop.run()
    }

    private val measureLoop = object : Runnable {
        override fun run() {
            if (!measuring) return
            elapsedSeconds++

            // BLE SENSOR 특성에서 흘러들어온 실제 total(f0+f1+f2) 값을 baseline과 비교해 분류
            val pressure = latestTotal.toFloat()
            pressureReadings.add(pressure)
            updateTensionState(latestTotal)

            // UI 업데이트
            val min = elapsedSeconds / 60
            val sec = elapsedSeconds % 60
            binding.tvTimer.text = String.format("%02d:%02d", min, sec)
            binding.tvCurrentPressure.text = String.format("%.0f", pressure)

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

    /** baseline 대비 total이 [TENSION_THRESHOLD_RATIO] 이상이면 긴장 상태로 분류한다. */
    private fun updateTensionState(total: Int) {
        val newState = if (baselineTotal > 0 && total >= baselineTotal * TENSION_THRESHOLD_RATIO) {
            TensionState.TENSE
        } else {
            TensionState.CALM
        }
        if (newState == tensionState) return
        tensionState = newState

        binding.tvTensionStatus.text = if (newState == TensionState.TENSE) "긴장 감지" else "이완 상태"
        binding.tvTensionStatus.setTextColor(
            Color.parseColor(if (newState == TensionState.TENSE) "#C97B63" else "#5A6B5A")
        )

        if (newState == TensionState.TENSE) sendTensionVibration()
    }

    private fun sendTensionVibration() {
        if (!prefs.isTensionVibrationEnabled) return
        val patternId = prefs.tensionVibrationPatternId ?: return
        val pattern = VibrationPatterns.ALL.firstOrNull { it.id == patternId } ?: return
        BleManager.sendVibration(pattern.effect)
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

    private companion object {
        const val TENSION_THRESHOLD_RATIO = 1.2f
    }
}
