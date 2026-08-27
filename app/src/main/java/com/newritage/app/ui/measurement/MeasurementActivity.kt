package com.newritage.app.ui.measurement

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.newritage.app.R
import com.newritage.app.ble.BleManager
import com.newritage.app.ble.BreathingCues
import com.newritage.app.ble.VibrationPatterns
import com.newritage.app.data.BreathingTechniques
import com.newritage.app.data.SensorReading
import com.newritage.app.data.SessionDataHolder
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityMeasurementBinding
import com.newritage.app.ui.main.HelpImageDialog
import com.newritage.app.ui.util.WaveStyle
import kotlin.math.roundToInt

class MeasurementActivity : AppCompatActivity() {

    private enum class TensionState { CALM, TENSE }
    private enum class Mode { AUTONOMOUS, GUIDE }

    private lateinit var binding: ActivityMeasurementBinding
    private lateinit var prefs: UserPreferences
    private lateinit var chartDataSet: LineDataSet
    private lateinit var mode: Mode

    private val breathingTechnique = BreathingTechniques.byId(BreathingTechniques.DEFAULT_ID)
    private var breathingStarted = false
    private var breathingPhaseIndex = 0
    private var breathingPhaseRemainingSec = 0

    private val handler = Handler(Looper.getMainLooper())
    private var measuring = false
    private var paused = false
    private var elapsedMillis = 0L
    private var elapsedSeconds = 0
    private var lastTimerSecond = -1
    private val pressureReadings = mutableListOf<Float>()
    private val sensorReadings = mutableListOf<SensorReading>()
    private var vibrationCount = 0

    // BLE 연결 직후 첫 실측 패킷이 도착하기 전까지 latestTotal은 초기값(0)이다. 이 0을 그대로
    // pressureReadings에 넣으면 실제로는 손가락을 누르고 있었어도 measurement delay 때문에
    // minPressure가 항상 0으로 기록되는 문제가 생긴다. 첫 실측 데이터를 받기 전까지는 통계에서
    // 제외한다.
    private var hasReceivedRealSensorData = false

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
        mode = if (intent.getStringExtra(EXTRA_MODE) == MODE_GUIDE) Mode.GUIDE else Mode.AUTONOMOUS

        // 홈 화면의 대기 링이 돌던 각도를 이어받아, 화면이 바뀌어도 링이 끊김없이 이어서 돌게 한다.
        val idleAngle = intent.getFloatExtra(EXTRA_IDLE_RING_ANGLE, 0f)
        binding.waveView.seedIdleAngle(idleAngle)

        binding.waveView.setWaveStyle(WaveStyle.MEASURING)
        setupNeedle()
        setupChart()
        positionLevelLabels()
        connectBle()
        startMeasurement()

        binding.btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        binding.btnStop.setOnClickListener { stopMeasurement() }
        binding.btnPause.setOnClickListener { togglePause() }
        binding.btnHelp.setOnClickListener { HelpImageDialog(this).show() }
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
            hasReceivedRealSensorData = true
        }
    }

    override fun onStop() {
        super.onStop()
        BleManager.onSensorData = null
    }

    /**
     * 바늘(ivNeedle)은 needle.xml 벡터 좌상단(0,0)이 뾰족한 끝 지점이 되도록 그려져 있다.
     * pivotX/Y를 0으로 두면 rotation을 걸어도 그 끝 지점의 화면 좌표는 x/y와 정확히 같게
     * 유지되므로, 매 tick마다 x/y만 그래프 마지막 데이터 포인트의 픽셀 좌표로 옮기면 된다.
     */
    private fun setupNeedle() {
        binding.ivNeedle.apply {
            pivotX = 0f
            pivotY = 0f
            rotation = NEEDLE_ROTATION_DEGREES
        }
    }

    private fun setupChart() {
        val axisLabelColor = ColorUtils.setAlphaComponent(
            ContextCompat.getColor(this, R.color.text_primary),
            (255 * 0.66f).roundToInt()
        )
        val axisTypeface = ResourcesCompat.getFont(this, R.font.spoqahansansneo_regular)

        binding.lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setBackgroundColor(Color.TRANSPARENT)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = axisLabelColor
            xAxis.typeface = axisTypeface
            xAxis.setDrawGridLines(false)
            axisLeft.textColor = axisLabelColor
            axisLeft.typeface = axisTypeface
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = chartAxisMaximum()
            // 기본 자동 라벨 개수(약 6~7개) 대비 절반 수준으로 줄여 가로 그리드선 간격을 넓힌다.
            axisLeft.setLabelCount(4, false)
            axisRight.isEnabled = false
            // 곡선·바늘·축이 오른쪽의 높음/적정/낮음 라벨(카드 우측 끝, tvLevelHigh 등)과
            // 겹치지 않도록, 플로팅 영역 자체를 오른쪽으로 이만큼 줄여서 라벨을 위한 여백을
            // 만든다. 값은 라벨 텍스트 폭(2글자, 10sp) + marginEnd(4dp)를 넉넉히 덮는
            // 근사치라 실제 화면에서 확인 후 조정이 필요할 수 있다.
            extraRightOffset = 40f
            // x축 라벨이 cardChart(FrameLayout)의 아래쪽 경계에 바짝 붙어 그려지면서 그
            // 아래의 완료 버튼과 시각적으로 붙어 보이는 것을 막기 위해 하단에도 여백을 둔다.
            extraBottomOffset = 12f
        }
    }

    /**
     * 세로축 최댓값(맨 위 줄)을 baseline 기준으로 잡아, 균등 간격 자동눈금의 2~4번째 줄이
     * 높음(1.3배)/적정(1.1배)/낮음(0.6배) 값 근처에 오도록 한다(정확한 일치는 아님 - 세
     * 값의 간격이 서로 달라 균등눈금으로는 딱 맞출 수 없다). 하드웨어 최댓값(센서 3개 ADC
     * 합산 최대치)을 넘지 않도록 상한을 둔다. baseline이 아직 없으면 하드웨어 최댓값을 그대로
     * 쓴다.
     */
    private fun chartAxisMaximum(): Float {
        if (baselineTotal <= 0) return CHART_DISPLAY_MAX_RAW
        return (baselineTotal * CHART_AXIS_MAX_OVER_BASELINE_RATIO).coerceAtMost(CHART_DISPLAY_MAX_RAW)
    }

    /**
     * 높음/적정/낮음 기준값의 세로 위치를 계산해 라벨을 배치한다. 예전에는 MPAndroidChart의
     * LimitLine(점선 + 라벨)을 축 안쪽에 그렸지만, 점선은 지우고 텍스트만 곡선·바늘보다
     * 오른쪽 바깥(카드 우측 끝)에 두기 위해 별도 오버레이 TextView로 직접 배치한다. 경계
     * 비율(1.1/1.3)은 ThreadColors의 낮음·보통·높음 분류 기준과 동일하게 맞췄다. 낮음
     * 기준선 위치는 고정 경계가 없어 baseline의 60%를 임의 기준으로 표시한다(TODO: 실기
     * 데이터로 조정). baseline과 축 범위(0~CHART_DISPLAY_MAX_RAW)는 세션 내내 고정이라
     * 위치도 최초 1회만 계산하면 된다(바늘처럼 매 tick 갱신할 필요 없음).
     */
    private fun positionLevelLabels() {
        val baselineRaw = baselineTotal.toFloat().coerceAtLeast(1f)
        binding.lineChart.post {
            val chart = binding.lineChart
            val transformer = chart.getTransformer(YAxis.AxisDependency.LEFT)
            positionLevelLabel(binding.tvLevelHigh, transformer.getPixelForValues(0f, baselineRaw * LEVEL_HIGH_RATIO).y)
            positionLevelLabel(binding.tvLevelModerate, transformer.getPixelForValues(0f, baselineRaw * LEVEL_MODERATE_RATIO).y)
            positionLevelLabel(binding.tvLevelLow, transformer.getPixelForValues(0f, baselineRaw * LEVEL_LOW_RATIO).y)
        }
    }

    private fun positionLevelLabel(label: TextView, pixelY: Double) {
        label.y = binding.lineChart.top + pixelY.toFloat() - label.height / 2f
    }

    /** 측정 시작 시 그래프를 빈 데이터셋으로 초기화한다. */
    private fun resetChartData() {
        chartDataSet = LineDataSet(mutableListOf(), "압력").apply {
            color = Color.parseColor("#81A68D")
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        binding.lineChart.data = LineData(chartDataSet)
        binding.lineChart.invalidate()
        // 새 세션 시작 시 아직 데이터가 없으므로, 첫 그래프 값이 들어올 때까지 바늘을 숨긴다.
        binding.ivNeedle.visibility = View.GONE
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
        hasReceivedRealSensorData = false
        tensionState = TensionState.CALM
        lastTensionVibrationAtMillis = -TENSION_COOLDOWN_MS
        timerVibrationFired = false
        breathingStarted = false
        breathingPhaseIndex = 0
        breathingPhaseRemainingSec = 0
        binding.btnStop.isEnabled = true
        binding.waveView.startWave()
        resetChartData()
        if (mode == Mode.GUIDE) {
            BleManager.sendVibration(BreathingCues.START)
        }
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
            // 첫 실측 패킷이 도착하기 전(hasReceivedRealSensorData == false) 틱은 latestTotal이
            // 초기값 0이라 통계에 넣지 않는다 - 그래야 measurement delay 때문에 minPressure가
            // 항상 0으로 나오는 문제가 생기지 않는다.
            if (hasReceivedRealSensorData) {
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
            }

            // 긴장 판정·타이머·진동은 노이즈에 흔들리지 않도록 실제 1초 경계에서만 갱신한다.
            if (elapsedSecondsNow != lastTimerSecond) {
                lastTimerSecond = elapsedSecondsNow
                elapsedSeconds = elapsedSecondsNow
                updateTensionState(latestTotal, allowVibration = mode == Mode.AUTONOMOUS)
                if (mode == Mode.GUIDE) advanceBreathingCue()
                if (!timerVibrationFired && elapsedSecondsNow >= prefs.timerDurationMinutes * 60) {
                    timerVibrationFired = true
                    sendTimerVibration()
                }
                val min = elapsedSecondsNow / 60
                val sec = elapsedSecondsNow % 60
                binding.tvTimer.text = String.format("%02d:%02d", min, sec)
            }

            // 압력값·물결·그래프는 바로바로 반영되도록 빠른 주기로 갱신 (raw 값 그대로 표시)
            binding.tvCurrentPressure.text = String.format("%.0f", pressure)

            // 웨이브뷰: baseline*2를 가득 찬 기준으로 삼아 setPressure()가 기대하는 0~80 스케일에 근사 매핑
            val waveInput = (pressure / (baselineTotal.coerceAtLeast(1) * 2f)).coerceIn(0f, 1f) * 80f
            binding.waveView.setPressure(waveInput)

            appendChartEntry(elapsedMillis / 1000f, pressure)

            handler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    private fun appendChartEntry(xSeconds: Float, pressureRaw: Float) {
        chartDataSet.addEntry(Entry(xSeconds, pressureRaw))
        while (chartDataSet.entryCount > 1 && xSeconds - chartDataSet.getEntryForIndex(0).x > CHART_WINDOW_SECONDS) {
            chartDataSet.removeFirst()
        }
        binding.lineChart.data?.notifyDataChanged()
        binding.lineChart.notifyDataSetChanged()
        binding.lineChart.invalidate()
        updateNeedlePosition(xSeconds, pressureRaw)
    }

    /**
     * 바늘 끝을 그래프 곡선의 마지막(가장 최신) 데이터 포인트 위로 옮긴다. 차트는 removeFirst로
     * 오래된 값만 밀어내고 x축 범위를 데이터에 맞춰 자동으로 재계산하므로, 최신 포인트는 항상
     * 차트 콘텐츠 영역의 오른쪽 끝 부근에 위치한다 - 즉 바늘은 그래프가 오르내리는 대로 세로로만
     * 움직이는 것처럼 보인다. lineChart.invalidate() 직후에는 Transformer가 아직 새 축 범위로
     * 갱신되지 않았을 수 있어, 실제 draw 패스가 끝난 뒤(post) 픽셀 좌표를 계산한다.
     */
    private fun updateNeedlePosition(xSeconds: Float, pressureRaw: Float) {
        binding.lineChart.post {
            val chart = binding.lineChart
            val pixel = chart.getTransformer(YAxis.AxisDependency.LEFT)
                .getPixelForValues(xSeconds, pressureRaw)
            binding.ivNeedle.apply {
                x = chart.left + pixel.x.toFloat()
                y = chart.top + pixel.y.toFloat()
                visibility = View.VISIBLE
            }
        }
    }

    /**
     * baseline 대비 total이 상향 임계 이상이면 긴장으로 분류한다. 연속 떨림 방지를 위해
     * 하향 여유선 아래로 내려오기 전까지는 CALM으로 되돌아가지 않는 히스테리시스를 적용하고,
     * 진동 발동 자체는 별도로 쿨다운을 둬 짧은 시간 내 재발동을 막는다.
     */
    private fun updateTensionState(total: Int, allowVibration: Boolean) {
        if (baselineTotal <= 0) return
        val upThreshold = baselineTotal * prefs.tensionThresholdRatio
        val downThreshold = upThreshold * (1f - TENSION_RELEASE_MARGIN_RATIO)

        when {
            tensionState == TensionState.CALM && total >= upThreshold -> {
                tensionState = TensionState.TENSE
                updateTensionStatusUi()
                if (allowVibration && elapsedMillis - lastTensionVibrationAtMillis >= TENSION_COOLDOWN_MS) {
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

    /**
     * 가이드 모드에서 긴장도 임계치 진동 대신 재생되는 호흡 리듬 진동 큐. 정해진 종료 없이
     * 완료 버튼을 누를 때까지 기법의 phase를 계속 순환한다.
     */
    private fun advanceBreathingCue() {
        if (!breathingStarted) {
            breathingStarted = true
            breathingPhaseIndex = 0
            breathingPhaseRemainingSec = breathingTechnique.phases[0].durationSec
            fireBreathingCue(breathingTechnique.phases[0].cue)
            return
        }
        breathingPhaseRemainingSec--
        if (breathingPhaseRemainingSec <= 0) {
            breathingPhaseIndex = (breathingPhaseIndex + 1) % breathingTechnique.phases.size
            val phase = breathingTechnique.phases[breathingPhaseIndex]
            breathingPhaseRemainingSec = phase.durationSec
            fireBreathingCue(phase.cue)
        }
    }

    private fun fireBreathingCue(cue: Int) {
        vibrationCount++
        BleManager.sendVibration(cue)
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
        if (mode == Mode.GUIDE) {
            BleManager.sendVibration(BreathingCues.END)
        }

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
        const val EXTRA_MODE = "extra_mode"
        const val MODE_AUTONOMOUS = "autonomous"
        const val MODE_GUIDE = "guide"

        // TODO: 실기 테스트 후 여유 비율·쿨다운 구체값 조정
        private const val TENSION_RELEASE_MARGIN_RATIO = 0.1f
        private const val TENSION_COOLDOWN_MS = 3000L

        private const val TICK_INTERVAL_MS = 100L
        private const val CHART_WINDOW_SECONDS = 60f
        // 바늘의 고정 회전각(시계방향, 도). needle.xml 원본이 이미 끝(뾰족한 부분)이 위쪽을
        // 향하고 구멍 있는 두꺼운 쪽이 아래로 늘어지는 모양으로 그려져 있어 추가 회전이 필요
        // 없다(0도) - 시안 이미지 속 바늘 아이콘의 각도와 동일하다. 끝은 pivotX/Y=0 덕분에
        // 이 값과 무관하게 항상 그래프 마지막 값 좌표에 고정된다.
        private const val NEEDLE_ROTATION_DEGREES = 0f
        // 센서 3개(엄지·검지중지·손바닥) 12비트 ADC 합산 최대치(4095 × 3). raw total 그대로 표시.
        private const val CHART_DISPLAY_MAX_RAW = 12285f
        // 높음/적정/낮음 판정 비율. ThreadColors의 낮음·보통·높음 분류 기준과 동일하게 맞췄다.
        // 낮음 기준선 위치는 고정 경계가 없어 baseline의 60%를 임의 기준으로 표시한다.
        private const val LEVEL_HIGH_RATIO = 1.3f
        private const val LEVEL_MODERATE_RATIO = 1.1f
        private const val LEVEL_LOW_RATIO = 0.6f
        // 세로축 최댓값 = baseline × 이 배율. chartAxisMaximum() 참고.
        private const val CHART_AXIS_MAX_OVER_BASELINE_RATIO = 1.75f
    }
}
