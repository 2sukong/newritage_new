package com.newritage.app.ui.measurement

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.newritage.app.R
import com.newritage.app.ble.BleManager
import com.newritage.app.ble.VibrationPatterns
import com.newritage.app.data.SensorReading
import com.newritage.app.data.SessionDataHolder
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityMeasurementBinding
import com.newritage.app.ui.util.WaveStyle
import com.newritage.app.util.PressureDisplay

class MeasurementActivity : AppCompatActivity() {

    private enum class TensionState { CALM, TENSE }

    private lateinit var binding: ActivityMeasurementBinding
    private lateinit var prefs: UserPreferences
    private lateinit var chartDataSet: LineDataSet

    private val handler = Handler(Looper.getMainLooper())
    private var measuring = false
    private var paused = false
    private var elapsedMillis = 0L
    private var elapsedSeconds = 0
    private var lastTimerSecond = -1
    private val pressureReadings = mutableListOf<Float>()
    private val sensorReadings = mutableListOf<SensorReading>()
    private var vibrationCount = 0

    // BLE SENSOR 특성에서 마지막으로 받은 f0(엄지)/f1(검지·중지)/f2(손바닥)/total 값
    private var latestThumb = 0
    private var latestIm = 0
    private var latestPalm = 0
    private var latestTotal = 0
    private var baselineTotal = 0
    private var tensionState = TensionState.CALM
    private var lastTensionVibrationAtMillis = -TENSION_COOLDOWN_MS
    private var timerVibrationFired = false
    private var sessionStartAtMillis = 0L

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

        // 홈 화면의 대기 링이 돌던 각도를 이어받아, 화면이 바뀌어도 링이 끊김없이 이어서 돌게 한다.
        val idleAngle = intent.getFloatExtra(EXTRA_IDLE_RING_ANGLE, 0f)
        binding.waveView.seedIdleAngle(idleAngle)

        binding.waveView.setWaveStyle(WaveStyle.MEASURING)
        setupChart()
        connectBle()
        startMeasurement()

        binding.btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        binding.btnStop.setOnClickListener { stopMeasurement() }
        binding.btnPause.setOnClickListener { togglePause() }
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
            axisLeft.axisMaximum = CHART_DISPLAY_MAX_KPA
            // 기본 자동 라벨 개수(약 6~7개) 대비 절반 수준으로 줄여 가로 그리드선 간격을 넓힌다.
            axisLeft.setLabelCount(4, false)
            axisLeft.setDrawLimitLinesBehindData(true)
            axisRight.isEnabled = false

            // baseline 대비 낮음/적정/높음 구간 참조선. 경계 비율(1.1/1.3)은 ThreadColors의
            // 낮음·보통·높음 분류 기준과 동일하게 맞췄다. 낮음 참조선 위치는 고정 경계가 없어
            // baseline의 60%를 임의 기준으로 표시한다(TODO: 실기 데이터로 조정).
            val baselineKpa = (baselineTotal / PressureDisplay.SCALE).coerceAtLeast(1f)
            axisLeft.removeAllLimitLines()
            axisLeft.addLimitLine(levelLimitLine(baselineKpa * 0.6f, getString(R.string.chart_level_low)))
            axisLeft.addLimitLine(levelLimitLine(baselineKpa * 1.1f, getString(R.string.chart_level_moderate)))
            axisLeft.addLimitLine(levelLimitLine(baselineKpa * 1.3f, getString(R.string.chart_level_high)))
        }
    }

    private fun levelLimitLine(valueKpa: Float, label: String): LimitLine {
        return LimitLine(valueKpa, label).apply {
            lineColor = Color.parseColor("#B5C9A5")
            lineWidth = 1f
            enableDashedLine(6f, 4f, 0f)
            textColor = Color.parseColor("#5A6B5A")
            textSize = 10f
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
        }
    }

    /** 측정 시작 시 그래프를 빈 데이터셋으로 초기화한다. */
    private fun resetChartData() {
        chartDataSet = LineDataSet(mutableListOf(), "압력").apply {
            color = Color.parseColor("#8B9E7B")
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            setDrawFilled(true)
            fillColor = Color.parseColor("#8B9E7B")
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        binding.lineChart.data = LineData(chartDataSet)
        binding.lineChart.invalidate()
    }

    private fun startMeasurement() {
        measuring = true
        paused = false
        sessionStartAtMillis = System.currentTimeMillis()
        elapsedMillis = 0L
        elapsedSeconds = 0
        lastTimerSecond = -1
        pressureReadings.clear()
        sensorReadings.clear()
        vibrationCount = 0
        tensionState = TensionState.CALM
        lastTensionVibrationAtMillis = -TENSION_COOLDOWN_MS
        timerVibrationFired = false
        binding.btnStop.isEnabled = true
        binding.waveView.startWave()
        resetChartData()
        measureLoop.run()
    }

    /** 일시정지 중에는 handler에 다음 tick이 예약되지 않으므로 측정 로직 전체가 멈춘다. */
    private fun togglePause() {
        if (!measuring) return
        paused = !paused
        if (paused) {
            handler.removeCallbacks(measureLoop)
            binding.waveView.stopWave()
            binding.btnPause.setImageResource(R.drawable.ic_play_circle)
        } else {
            binding.waveView.startWave()
            binding.btnPause.setImageResource(R.drawable.ic_pause_circle)
            handler.postDelayed(measureLoop, TICK_INTERVAL_MS)
        }
    }

    private val measureLoop = object : Runnable {
        override fun run() {
            if (!measuring) return
            elapsedMillis += TICK_INTERVAL_MS
            val elapsedSecondsNow = (elapsedMillis / 1000).toInt()

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

            // 긴장 판정·타이머·진동은 노이즈에 흔들리지 않도록 실제 1초 경계에서만 갱신한다.
            if (elapsedSecondsNow != lastTimerSecond) {
                lastTimerSecond = elapsedSecondsNow
                elapsedSeconds = elapsedSecondsNow
                updateTensionState(latestTotal)
                if (!timerVibrationFired && elapsedSecondsNow >= prefs.timerDurationMinutes * 60) {
                    timerVibrationFired = true
                    sendTimerVibration()
                }
                val min = elapsedSecondsNow / 60
                val sec = elapsedSecondsNow % 60
                binding.tvTimer.text = String.format("%02d:%02d", min, sec)
            }

            // 압력값·물결·그래프는 바로바로 반영되도록 빠른 주기로 갱신 (표시는 kPa 스케일로 환산)
            val pressureKpa = pressure / PressureDisplay.SCALE
            binding.tvCurrentPressure.text = String.format("%.0f", pressureKpa)

            // 웨이브뷰: baseline*2를 가득 찬 기준으로 삼아 setPressure()가 기대하는 0~80 스케일에 근사 매핑
            val waveInput = (pressure / (baselineTotal.coerceAtLeast(1) * 2f)).coerceIn(0f, 1f) * 80f
            binding.waveView.setPressure(waveInput)

            appendChartEntry(elapsedMillis / 1000f, pressureKpa)

            handler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    private fun appendChartEntry(xSeconds: Float, pressureKpa: Float) {
        chartDataSet.addEntry(Entry(xSeconds, pressureKpa))
        while (chartDataSet.entryCount > 1 && xSeconds - chartDataSet.getEntryForIndex(0).x > CHART_WINDOW_SECONDS) {
            chartDataSet.removeFirst()
        }
        binding.lineChart.data?.notifyDataChanged()
        binding.lineChart.notifyDataSetChanged()
        binding.lineChart.invalidate()
    }

    /**
     * baseline 대비 total이 상향 임계 이상이면 긴장으로 분류한다. 연속 떨림 방지를 위해
     * 하향 여유선 아래로 내려오기 전까지는 CALM으로 되돌아가지 않는 히스테리시스를 적용하고,
     * 진동 발동 자체는 별도로 쿨다운을 둬 짧은 시간 내 재발동을 막는다.
     */
    private fun updateTensionState(total: Int) {
        if (baselineTotal <= 0) return
        val upThreshold = baselineTotal * prefs.tensionThresholdRatio
        val downThreshold = upThreshold * (1f - TENSION_RELEASE_MARGIN_RATIO)

        when {
            tensionState == TensionState.CALM && total >= upThreshold -> {
                tensionState = TensionState.TENSE
                updateTensionStatusUi()
                if (elapsedMillis - lastTensionVibrationAtMillis >= TENSION_COOLDOWN_MS) {
                    lastTensionVibrationAtMillis = elapsedMillis
                    vibrationCount++
                    sendTensionVibration()
                }
            }
            tensionState == TensionState.TENSE && total <= downThreshold -> {
                tensionState = TensionState.CALM
                updateTensionStatusUi()
            }
        }
    }

    private fun updateTensionStatusUi() {
        val isTense = tensionState == TensionState.TENSE
        binding.tvTensionStatus.text = if (isTense) "긴장 감지" else "이완 상태"
        binding.tvTensionStatus.setTextColor(Color.parseColor(if (isTense) "#C97B63" else "#5A6B5A"))
        binding.tvStatusDesc.text = getString(
            if (isTense) R.string.pressure_status_tense_desc else R.string.pressure_status_calm_desc
        )
    }

    private fun sendTensionVibration() {
        if (!prefs.isTensionVibrationEnabled) return
        val patternId = prefs.tensionVibrationPatternId ?: VibrationPatterns.TENSION_DEFAULT_ID
        val pattern = VibrationPatterns.ALL.firstOrNull { it.id == patternId } ?: return
        BleManager.sendVibration(pattern.effect, pattern.count, pattern.intervalMs)
    }

    /** 카운트다운(prefs.timerDurationMinutes)이 0에 도달하는 순간 1회만 타이머 진동을 울린다. */
    private fun sendTimerVibration() {
        if (!prefs.isTimerVibrationEnabled) return
        val patternId = prefs.timerVibrationPatternId ?: VibrationPatterns.TIMER_DEFAULT_ID
        val pattern = VibrationPatterns.ALL.firstOrNull { it.id == patternId } ?: return
        BleManager.sendVibration(pattern.effect, pattern.count, pattern.intervalMs)
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
            putExtra("session_start_millis", sessionStartAtMillis)
            putExtra("session_end_millis", System.currentTimeMillis())
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        measuring = false
        handler.removeCallbacks(measureLoop)
    }

    companion object {
        const val EXTRA_IDLE_RING_ANGLE = "idle_ring_angle"

        // TODO: 실기 테스트 후 여유 비율·쿨다운 구체값 조정
        private const val TENSION_RELEASE_MARGIN_RATIO = 0.1f
        private const val TENSION_COOLDOWN_MS = 3000L

        private const val TICK_INTERVAL_MS = 100L
        private const val CHART_WINDOW_SECONDS = 60f
        private const val CHART_DISPLAY_MAX_KPA = 60f
    }
}
