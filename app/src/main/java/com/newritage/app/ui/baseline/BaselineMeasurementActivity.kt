package com.newritage.app.ui.baseline

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.newritage.app.R
import com.newritage.app.ble.BleManager
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityBaselineMeasurementBinding
import com.newritage.app.ui.main.MainActivity
import com.newritage.app.ui.util.WaveStyle
import com.newritage.app.util.FeatureFlags

class BaselineMeasurementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBaselineMeasurementBinding
    private lateinit var prefs: UserPreferences

    private val handler = Handler(Looper.getMainLooper())
    private var measuring = false
    private var elapsedMillis = 0L

    private val measureDuration = 10
    private val pressureReadings = mutableListOf<Float>()
    private val thumbReadings = mutableListOf<Float>()
    private val imReadings = mutableListOf<Float>()
    private val palmReadings = mutableListOf<Float>()

    // BLE SENSOR 특성에서 마지막으로 받은 f0(엄지)/f1(검지·중지)/f2(손바닥)/total 값
    private var latestThumb = 0
    private var latestIm = 0
    private var latestPalm = 0
    private var latestTotal = 0

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) {
                BleManager.startScan(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaselineMeasurementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)

        setupUI()
        connectBle()
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
        updateConnectionBadge(BleManager.isConnected)
        BleManager.onConnectionChange = { connected -> updateConnectionBadge(connected) }
    }

    override fun onStop() {
        super.onStop()
        BleManager.onSensorData = null
        BleManager.onConnectionChange = null
    }

    private fun updateConnectionBadge(connected: Boolean) {
        binding.tvConnectionStatusReady.text = getString(
            if (connected) R.string.connection_connected else R.string.connection_disconnected
        )
        binding.dotConnectionReady.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, if (connected) R.color.primary else R.color.text_hint)
        )
    }

    private fun setupUI() {
        // 최초 상태는 준비 화면(Screen.GUIDE)으로 세팅
        showScreen(Screen.GUIDE)

        // 초기값 설정에 들어가기 앞서 측정 안내 바텀시트를 자동으로 띄운다
        binding.root.post { showGuideSheet() }
        binding.btnMeasureTip.setOnClickListener { showGuideSheet() }

        // 아까 바꾼 새로운 XML 디자인 속 '시작하기' 버튼 연결
        binding.btnStartMeasure.setOnClickListener {
            if (FeatureFlags.REQUIRE_BLE_CONNECTION_TO_START && !BleManager.isConnected) {
                Toast.makeText(this, R.string.connection_required_toast, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            animateReadyCircleIntoMeasuring()
            startMeasurement()
        }

        // 아까 바꾼 새로운 XML 디자인 속 '메인화면으로' 버튼 연결
        binding.btnGoMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    /**
     * guideCircleReady와 gaugeCircleContainer는 레이아웃상 완전히 같은 위치에 겹쳐 있으므로
     * 원을 옮기는 애니메이션 없이 준비 화면 전체를 페이드아웃, 측정 화면을 페이드인만 시켜도
     * 같은 자리에서 바로 물결 모션이 시작되는 것처럼 보인다.
     */
    private fun animateReadyCircleIntoMeasuring() {
        binding.layoutMeasuring.visibility = View.VISIBLE
        binding.layoutMeasuring.alpha = 0f

        binding.layoutReady.animate()
            .alpha(0f)
            .setDuration(TRANSITION_DURATION_MS)
            .withEndAction {
                binding.layoutReady.visibility = View.INVISIBLE
                binding.layoutReady.alpha = 1f
            }
            .start()

        binding.layoutMeasuring.animate()
            .alpha(1f)
            .setDuration(TRANSITION_DURATION_MS)
            .start()
    }

    private fun startMeasurement() {
        measuring = true
        elapsedMillis = 0L
        pressureReadings.clear()
        thumbReadings.clear()
        imReadings.clear()
        palmReadings.clear()

        binding.waveView.setWaveStyle(WaveStyle.MEASURING)
        binding.waveView.startWave()

        measureLoop.run()
    }

    private val measureLoop = object : Runnable {
        override fun run() {
            if (!measuring) return

            elapsedMillis += TICK_INTERVAL_MS
            val elapsedSecondsNow = (elapsedMillis / 1000).toInt()

            // BLE SENSOR 특성에서 흘러들어온 실제 f0/f1/f2/total 값을 그대로 사용
            val rawPressure = latestTotal.toFloat()
            pressureReadings.add(rawPressure)
            thumbReadings.add(latestThumb.toFloat())
            imReadings.add(latestIm.toFloat())
            palmReadings.add(latestPalm.toFloat())

            // 1. 하단 흰색 박스 안의 타이머 분:초 갱신 (tvLiveTimer)
            binding.tvLiveTimer.text = String.format("%d:%02d", elapsedSecondsNow / 60, elapsedSecondsNow % 60)

            // 2. 중앙에 실시간 수신된 단일 압력값 표기 (정수형 예시), 물결도 바로바로 반영되도록 빠른 주기로 갱신
            binding.tvLivePressureValue.text = String.format("%d", rawPressure.toInt())

            // 3. 원 내부의 물결 높이를 실시간 압력에 맞춰 조절 (최댓값을 8000 기준으로 0~80 스케일로 정규화)
            //    setPressure()가 내부적으로 스프링 보간을 하기 때문에 값이 급변해도 물결 높이는 부드럽게 이어진다.
            val waveInput = (rawPressure / MEASURING_PRESSURE_SCALE_MAX).coerceIn(0f, 1f) * 80f
            binding.waveView.setPressure(waveInput)

            if (elapsedSecondsNow >= measureDuration) {
                completeMeasurement()
            } else {
                handler.postDelayed(this, TICK_INTERVAL_MS)
            }
        }
    }

    private fun completeMeasurement() {
        measuring = false
        handler.removeCallbacks(measureLoop)
        binding.waveView.stopWave()

        prefs.baselineOverall = pressureReadings.average().toFloat()
        prefs.baselineThumb = thumbReadings.average().toFloat()
        prefs.baselineIM = imReadings.average().toFloat()
        prefs.baselinePalm = palmReadings.average().toFloat()
        prefs.isBaselineDone = true

        // 세 번째 완료 화면 중앙 원 안에 최종 결과 표기
        // 원본 이미지 가이드 멘트에 맞게 최종 평균값 매핑
        showScreen(Screen.COMPLETE)
    }

    private fun showGuideSheet() {
        BaselineGuideBottomSheetDialog(this).show()
    }

    private fun showScreen(screen: Screen) {
        // INVISIBLE을 사용해 항상 레이아웃이 계산되도록 유지해, 화면 전환 시 다시 레이아웃
        // 패스를 기다리지 않고 바로 크로스페이드할 수 있게 한다.
        binding.layoutReady.visibility = if (screen == Screen.GUIDE) View.VISIBLE else View.INVISIBLE
        binding.layoutMeasuring.visibility = if (screen == Screen.MEASURING) View.VISIBLE else View.INVISIBLE
        binding.layoutComplete.visibility = if (screen == Screen.COMPLETE) View.VISIBLE else View.INVISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        measuring = false
        handler.removeCallbacks(measureLoop)
    }

    enum class Screen { GUIDE, MEASURING, COMPLETE }

    private companion object {
        const val TICK_INTERVAL_MS = 100L
        const val MEASURING_PRESSURE_SCALE_MAX = 8000f
        const val TRANSITION_DURATION_MS = 450L
    }
}
