package com.newritage.app.ui.baseline

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.ble.BleManager
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityBaselineMeasurementBinding
import com.newritage.app.ui.main.MainActivity
import com.newritage.app.ui.util.WaveStyle

class BaselineMeasurementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBaselineMeasurementBinding
    private lateinit var prefs: UserPreferences

    private val handler = Handler(Looper.getMainLooper())
    private var measuring = false
    private var elapsedSeconds = 0

    // 30초에서 3분(180초) 측정으로 변경
    private val measureDuration = 180
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
    }

    override fun onStop() {
        super.onStop()
        BleManager.onSensorData = null
    }

    private fun setupUI() {
        // 최초 상태는 준비 화면(Screen.GUIDE)으로 세팅
        showScreen(Screen.GUIDE)

        // 아까 바꾼 새로운 XML 디자인 속 '시작하기' 버튼 연결
        binding.btnStartMeasure.setOnClickListener {
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

    private fun startMeasurement() {
        showScreen(Screen.MEASURING)
        measuring = true
        elapsedSeconds = 0
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

            elapsedSeconds++

            // BLE SENSOR 특성에서 흘러들어온 실제 f0/f1/f2/total 값을 그대로 사용
            val rawPressure = latestTotal.toFloat()
            pressureReadings.add(rawPressure)
            thumbReadings.add(latestThumb.toFloat())
            imReadings.add(latestIm.toFloat())
            palmReadings.add(latestPalm.toFloat())

            val elapsed = elapsedSeconds

            // 1. 하단 흰색 박스 안의 타이머 분:초 갱신 (tvLiveTimer)
            binding.tvLiveTimer.text = String.format("%d:%02d", elapsed / 60, elapsed % 60)

            // 2. 중앙에 실시간 수신된 단일 압력값 표기 (정수형 예시)
            binding.tvLivePressureValue.text = String.format("%d", rawPressure.toInt())

            // 3. 원 내부의 물결 높이를 실시간 압력에 맞춰 조절 (센서 이론상 최댓값 12285 기준 0~80 스케일로 정규화)
            val waveInput = (rawPressure / 12285f).coerceIn(0f, 1f) * 80f
            binding.waveView.setPressure(waveInput)

            if (elapsedSeconds >= measureDuration) {
                completeMeasurement()
            } else {
                handler.postDelayed(this, 1000L)
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

    private fun showScreen(screen: Screen) {
        // 새로 매칭된 ID 체계에 따라 화면 가시성 조절
        binding.layoutReady.visibility = if (screen == Screen.GUIDE) View.VISIBLE else View.GONE
        binding.layoutMeasuring.visibility = if (screen == Screen.MEASURING) View.VISIBLE else View.GONE
        binding.layoutComplete.visibility = if (screen == Screen.COMPLETE) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        measuring = false
        handler.removeCallbacks(measureLoop)
    }

    enum class Screen { GUIDE, MEASURING, COMPLETE }
}
