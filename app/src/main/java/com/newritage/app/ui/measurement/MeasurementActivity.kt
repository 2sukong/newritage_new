package com.newritage.app.ui.measurement

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.newritage.app.ble.BleManager
import com.newritage.app.ble.VibrationPatterns
import com.newritage.app.data.SensorReading
import com.newritage.app.data.SessionDataHolder
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityMeasurementBinding
import com.newritage.app.ui.util.WaveStyle

class MeasurementActivity : AppCompatActivity() {

    private enum class TensionState { CALM, TENSE }

    private lateinit var binding: ActivityMeasurementBinding
    private lateinit var prefs: UserPreferences

    private val handler = Handler(Looper.getMainLooper())
    private var measuring = false
    private var elapsedSeconds = 0
    private val pressureReadings = mutableListOf<Float>()
    private val sensorReadings = mutableListOf<SensorReading>()
    private val chartEntries = mutableListOf<Entry>()
    private var vibrationCount = 0

    // BLE SENSOR 특성에서 마지막으로 받은 f0(엄지)/f1(검지·중지)/f2(손바닥)/total 값
    private var latestThumb = 0
    private var latestIm = 0
    private var latestPalm = 0
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
        baselineTotal = prefs.baselineOverall.toInt()

        binding.waveView.setWaveStyle(WaveStyle.MEASURING)
        setupChart()
        connectBle()
        startMeasurement()

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
        BleManager.onSensorData = { f0, f1, f2, total ->
            latestThumb = f0
            latestIm = f1
            latestPalm = f2
            latestTotal = total
        }
    }

    override fun onStop() {
        super.onStop()
        BleManager.onSensorData = null
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
        sensorReadings.clear()
        chartEntries.clear()
        vibrationCount = 0
        tensionState = TensionState.CALM
        binding.btnStop.isEnabled = true
        binding.waveView.startWave()
        measureLoop.run()
    }

    private val measureLoop = object : Runnable {
        override fun run() {
            if (!measuring) return
            elapsedSeconds++

            // BLE SENSOR 특성에서 흘러들어온 실제 total(f0+f1+f2) 값을 baseline과 비교해 분류
            val pressure = latestTotal.toFloat()
            pressureReadings.add(pressure)
            sensorReadings.add(
                SensorReading(
                    sessionId = 0L, // SessionCompleteActivity에서 실제 세션 저장 후 채워짐
                    timestamp = System.currentTimeMillis(),
                    thumb = latestThumb.toFloat(),
                    indexMiddle = latestIm.toFloat(),
                    palm = latestPalm.toFloat(),
                    overall = pressure
                )
            )
            updateTensionState(latestTotal)
            if (elapsedSeconds % TIMER_VIBRATION_INTERVAL_SECONDS == 0) {
                sendTimerVibration()
            }

            // UI 업데이트
            val min = elapsedSeconds / 60
            val sec = elapsedSeconds % 60
            binding.tvTimer.text = String.format("%02d:%02d", min, sec)
            binding.tvCurrentPressure.text = String.format("%.0f", pressure)

            // 웨이브뷰: baseline*2를 가득 찬 기준으로 삼아 setPressure()가 기대하는 0~80 스케일에 근사 매핑
            val waveInput = (pressure / (baselineTotal.coerceAtLeast(1) * 2f)).coerceIn(0f, 1f) * 80f
            binding.waveView.setPressure(waveInput)

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

        if (newState == TensionState.TENSE) {
            vibrationCount++
            sendTensionVibration()
        }
    }

    private fun sendTensionVibration() {
        if (!prefs.isTensionVibrationEnabled) return
        val patternId = prefs.tensionVibrationPatternId ?: return
        val pattern = VibrationPatterns.ALL.firstOrNull { it.id == patternId } ?: return
        BleManager.sendVibration(pattern.effect)
    }

    /** 설정한 시간(기본 60초)마다 한 번씩 타이머 진동을 울린다. */
    private fun sendTimerVibration() {
        if (!prefs.isTimerVibrationEnabled) return
        val patternId = prefs.timerVibrationPatternId ?: return
        val pattern = VibrationPatterns.ALL.firstOrNull { it.id == patternId } ?: return
        BleManager.sendVibration(pattern.effect)
    }

    private fun stopMeasurement() {
        measuring = false
        handler.removeCallbacks(measureLoop)
        binding.waveView.stopWave()

        if (pressureReadings.isEmpty()) {
            finish()
            return
        }

        val avgPressure = pressureReadings.average().toFloat()
        val maxPressure = pressureReadings.max()
        val minPressure = pressureReadings.min()

        SessionDataHolder.sensorReadings = sensorReadings.toList()
        SessionDataHolder.vibrationCount = vibrationCount

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
        const val TIMER_VIBRATION_INTERVAL_SECONDS = 60
    }
}
