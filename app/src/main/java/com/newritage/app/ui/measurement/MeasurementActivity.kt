package com.newritage.app.ui.measurement

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.newritage.app.R
import com.newritage.app.ble.BleManager
import com.newritage.app.data.SensorReading
import com.newritage.app.databinding.ActivityMeasurementBinding
import com.newritage.app.ui.main.MainActivity
import com.newritage.app.ui.util.WaveViewNew
import java.text.SimpleDateFormat
import java.util.*

class MeasurementActivity : AppCompatActivity() {

    private enum class Screen { WAITING, MEASURING }

    private enum class MeasureMode { AUTO, GUIDE }

    private lateinit var binding: ActivityMeasurementBinding
    private var currentMode = MeasureMode.AUTO

    // Views
    private lateinit var screenWaiting: View
    private lateinit var screenMeasuring: View
    private lateinit var waveView: WaveViewNew
    private lateinit var tvPressureValue: TextView
    private lateinit var tvSessionTime: TextView
    private lateinit var tvFeedbackStatus: TextView
    private lateinit var lineChart: LineChart
    private lateinit var btnComplete: Button
    private lateinit var imgPauseResume: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private var countdownLeft = 10
    private var elapsedSeconds = 0
    private var deviationCount = 0       // 안정 상태 이탈 횟수
    private var isPaused = false

    private val pressureEntries = mutableListOf<Entry>()

    // BLE에서 들어오는 3개 센서 값 (엄지 / 검지·중지 / 손바닥)
    private var lastThumb = 0f
    private var lastIndexMiddle = 0f
    private var lastPalm = 0f

    // 3개 센서 값의 평균 = 현재 압력값
    private var currentPressure = 0f

    private val allPressures = mutableListOf<Float>()
    private val collectedReadings = mutableListOf<SensorReading>()

    private val startTimeStr by lazy {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeasurementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindViews()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        btnComplete.setOnClickListener { finishSession() }
        findViewById<View>(R.id.btnPauseResume).setOnClickListener { togglePauseResume() }

        binding.btnAutoMode.setOnClickListener {
            currentMode = MeasureMode.AUTO
            updateModeUI()
        }
        binding.btnGuideMode.setOnClickListener {
            currentMode = MeasureMode.GUIDE
            updateModeUI()
        }
        updateModeUI()

        findViewById<ImageView>(R.id.imgMeasureStart).setOnClickListener {
            showMeasuring()
        }

        // 도움말 버튼 로직 추가
        val helpOverlay = findViewById<View>(R.id.layoutHelpOverlay)
        findViewById<View>(R.id.btnHelp).setOnClickListener {
            helpOverlay.visibility = View.VISIBLE
        }
        helpOverlay.setOnClickListener {
            helpOverlay.visibility = View.GONE
        }

        // 탭바: 다른 메뉴 누르면 MainActivity로 돌아가서 해당 탭 선택
        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            val intent = Intent(this, MainActivity::class.java)
            if (item.itemId != R.id.nav_home) {
                intent.putExtra("select_tab", item.itemId)
            }
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
            true
        }

        val autoStart = intent.getBooleanExtra("auto_start", false)
        if (autoStart) {
            showMeasuring()
        } else {
            showWaiting()
        }
    }

    // ── BLE 연결 수명주기 ──────────────────────

    override fun onStart() {
        super.onStart()
        BleManager.onSensorData = { f0, f1, f2, _ ->
            lastThumb = f0.toFloat()
            lastIndexMiddle = f1.toFloat()
            lastPalm = f2.toFloat()
            currentPressure = (f0 + f1 + f2) / 3f
        }
    }

    override fun onStop() {
        super.onStop()
        BleManager.onSensorData = null
    }

    private fun bindViews() {
        screenWaiting = findViewById(R.id.screenWaiting)
        screenMeasuring = findViewById(R.id.screenMeasuring)
        waveView = findViewById(R.id.waveView)
        tvPressureValue = findViewById(R.id.tvPressureValue)
        tvSessionTime = findViewById(R.id.tvSessionTime)
        tvFeedbackStatus = findViewById(R.id.tvFeedbackStatus)
        lineChart = findViewById(R.id.lineChart)
        btnComplete = findViewById(R.id.btnSessionComplete)
        imgPauseResume = findViewById(R.id.imgPauseResume)
    }

    // ── 화면 전환 ──────────────────────────────

    private fun showWaiting() {
        screenWaiting.visibility = View.VISIBLE
        screenMeasuring.visibility = View.GONE
    }

    private fun showMeasuring() {
        screenWaiting.visibility = View.GONE
        screenMeasuring.visibility = View.VISIBLE
        setupChart()
        startMeasuring()
    }

    // ── 자율모드 / 가이드모드 토글 ─────────────

    private fun updateModeUI() {
        when (currentMode) {
            MeasureMode.AUTO -> {
                binding.btnAutoMode.setBackgroundResource(R.drawable.bg_mode_selected)
                binding.btnAutoMode.setTextColor(ContextCompat.getColor(this, R.color.white))

                binding.btnGuideMode.setBackgroundResource(R.drawable.bg_mode_unselected)
                binding.btnGuideMode.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
            MeasureMode.GUIDE -> {
                binding.btnGuideMode.setBackgroundResource(R.drawable.bg_mode_selected)
                binding.btnGuideMode.setTextColor(ContextCompat.getColor(this, R.color.white))

                binding.btnAutoMode.setBackgroundResource(R.drawable.bg_mode_unselected)
                binding.btnAutoMode.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
    }

    // ── 10초 카운트다운 → 자동 전환 ──────────────

    private fun startCountdown() {
        handler.post(object : Runnable {
            override fun run() {
                if (countdownLeft > 0) {
                    countdownLeft--
                    handler.postDelayed(this, 1000L)
                } else {
                    showMeasuring()
                }
            }
        })
    }

    // ── MPAndroidChart 설정 ───────────────────

    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            legend.isEnabled = false
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#AAAAAA")
                axisLineColor = Color.parseColor("#E0E0E0")
                textSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(v: Float): String {
                        val m = (v / 60).toInt(); val s = (v % 60).toInt()
                        return "%02d:%02d".format(m, s)
                    }
                }
                labelCount = 4
            }
            axisLeft.apply {
                axisMinimum = 0f; axisMaximum = 80f
                textColor = Color.parseColor("#AAAAAA")
                textSize = 10f
                gridColor = Color.parseColor("#F0F0F0")
                axisLineColor = Color.parseColor("#E0E0E0")
                // 참조선: 높음 / 적정 / 낮음
                addLimitLine(LimitLine(60f, "높음").apply {
                    lineColor = Color.parseColor("#F0E0E0")
                    lineWidth = 0.8f; textColor = Color.parseColor("#AAAAAA")
                    textSize = 9f; labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                })
                addLimitLine(LimitLine(35f, "적정").apply {
                    lineColor = Color.parseColor("#E8EDE4")
                    lineWidth = 0.8f; textColor = Color.parseColor("#8B9E7B")
                    textSize = 9f; labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                })
                addLimitLine(LimitLine(15f, "낮음").apply {
                    lineColor = Color.parseColor("#D0D0D0")
                    lineWidth = 0.8f; textColor = Color.parseColor("#AAAAAA")
                    textSize = 9f; labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                })
            }
            axisRight.isEnabled = false
        }
    }

    // ── 실시간 압력 기록 (BLE 평균값 기반) ─────

    private val measuringRunnable = object : Runnable {
        override fun run() {
            elapsedSeconds++

            allPressures.add(currentPressure)

            // 센서별 실측값 기록 (SessionCompleteActivity/통계용)
            collectedReadings.add(
                SensorReading(
                    sessionId = 0, // SessionCompleteActivity에서 저장 후 할당됨
                    timestamp = System.currentTimeMillis(),
                    thumb = lastThumb,
                    indexMiddle = lastIndexMiddle,
                    palm = lastPalm,
                    overall = currentPressure
                )
            )

            // 이탈 횟수 카운트 (>50kPa 를 '이탈'로 정의)
            if (currentPressure > 50f) deviationCount++

            // UI 갱신
            tvPressureValue.text = "%.0f".format(currentPressure)
            tvFeedbackStatus.text = feedbackText(currentPressure)
            tvSessionTime.text = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

            // 차트 갱신 (최근 60포인트)
            if (pressureEntries.size >= 60) pressureEntries.removeAt(0)
            pressureEntries.add(Entry(elapsedSeconds.toFloat(), currentPressure))
            updateChart()

            handler.postDelayed(this, 1000L)
        }
    }

    private fun startMeasuring() {
        isPaused = false
        imgPauseResume.setImageResource(R.drawable.ic_pause)
        handler.removeCallbacks(measuringRunnable)
        handler.post(measuringRunnable)
    }

    // ── 일시정지 / 재개 ────────────────────────

    private fun togglePauseResume() {
        isPaused = !isPaused
        if (isPaused) {
            handler.removeCallbacks(measuringRunnable)
            waveView.stopWave()
            imgPauseResume.setImageResource(R.drawable.ic_play)
        } else {
            handler.post(measuringRunnable)
            waveView.startWave()
            imgPauseResume.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun feedbackText(p: Float) = when {
        p < 25f -> "긴장이 많이 완화되었습니다."
        p < 45f -> "적절 범위 내의 편안한 압력입니다."
        else    -> "호흡을 고르게 하고 긴장을 풀어보세요."
    }

    private fun updateChart() {
        val ds = LineDataSet(ArrayList(pressureEntries), "압력").apply {
            color = Color.parseColor("#9DB18E"); lineWidth = 1.5f
            setDrawCircles(false); setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(false)
        }
        lineChart.data = LineData(ds)
        lineChart.invalidate()
    }

    // ── 완료 → SessionCompleteActivity ─────────

    private fun finishSession() {
        handler.removeCallbacksAndMessages(null)
        val endTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val avg = if (allPressures.isEmpty()) 0f else allPressures.average().toFloat()
        val max = allPressures.maxOrNull() ?: avg
        val min = allPressures.minOrNull() ?: avg

        // 원시 데이터를 Holder에 저장 (Intent 용량 제한 회피)
        com.newritage.app.data.SessionDataHolder.sensorReadings = collectedReadings

        startActivity(Intent(this, SessionCompleteActivity::class.java).apply {
            putExtra("duration_seconds", elapsedSeconds)
            putExtra("avg_pressure", avg)
            putExtra("max_pressure", max)
            putExtra("min_pressure", min)
            putExtra("deviation_count", deviationCount)
            putExtra("start_time", startTimeStr)
            putExtra("end_time", endTime)
        })
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        BleManager.onSensorData = null
    }
}