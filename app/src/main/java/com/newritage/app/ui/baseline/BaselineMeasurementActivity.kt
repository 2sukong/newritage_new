package com.newritage.app.ui.baseline

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.newritage.app.R
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityBaselineMeasurementBinding
import com.newritage.app.ui.main.MainActivity
import kotlin.random.Random

class BaselineMeasurementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBaselineMeasurementBinding
    private lateinit var prefs: UserPreferences

    private val handler = Handler(Looper.getMainLooper())
    private var measuring = false
    private var elapsedSeconds = 0

    // 30초에서 3분(180초) 측정으로 변경
    private val measureDuration = 180
    private val pressureReadings = mutableListOf<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaselineMeasurementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)

        setupUI()

        // 🌟 수정: 화면 레이아웃 배치가 완전히 끝난 직후(Queue에 들어간 후) 안전하게 팝업을 띄우도록 핸들러 활용
        handler.post {
            if (!isFinishing) { // 액티비티가 살아있는지 검증
                showMeasurementGuideDialog()
            }
        }
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

    /**
     * 팝업 다이얼로그를 생성하고 띄우는 함수
     */
    private fun showMeasurementGuideDialog() {
        val dialog = BottomSheetDialog(this)

        // 미리 만들어둔 dialog_baseline_measurement_complete.xml 주입
        val dialogView = layoutInflater.inflate(R.layout.dialog_baseline_measurement_complete, null)
        dialog.setContentView(dialogView)

        // 🌟 1. 팝업 화면 전체(최상위 레이아웃)를 터치하면 닫히도록 설정
        dialogView.setOnClickListener {
            dialog.dismiss() // 팝업창 닫기 ➔ 뒤에 있던 준비 화면이 보임
        }

        // 🌟 2. 팝업창 바깥의 어두운 빈 공간을 터치해도 자동으로 꺼지도록 변경 (true)
        dialog.setCanceledOnTouchOutside(true)

        dialog.show()
    }

    private fun startMeasurement() {
        showScreen(Screen.MEASURING)
        measuring = true
        elapsedSeconds = 0
        pressureReadings.clear()

        // 새로 교체한 원형 프로그레스 바(circularProgressBar) 맥스값 설정
        binding.circularProgressBar.max = measureDuration
        binding.circularProgressBar.progress = 0

        measureLoop.run()
    }

    private val measureLoop = object : Runnable {
        override fun run() {
            if (!measuring) return

            elapsedSeconds++

            // 💡 [실제 하드웨어 연결 구간]
            // 나중에 블루투스로 받아올 실제 압력값(mmHg 단위)이 들어갈 자리입니다.
            // 현재는 테스트를 위해 랜덤 값(mmHg 가상 데이터)으로 시뮬레이션합니다.
            val rawPressure = 35f + Random.nextFloat() * 15f
            pressureReadings.add(rawPressure)

            val elapsed = elapsedSeconds

            // 1. 하단 흰색 박스 안의 타이머 분:초 갱신 (tvLiveTimer)
            binding.tvLiveTimer.text = String.format("%d:%02d", elapsed / 60, elapsed % 60)

            // 2. 원형 프로그레스 바 테두리 1초씩 차오르게 하기
            binding.circularProgressBar.progress = elapsed

            // 3. 중앙에 실시간 수신된 단일 압력값 표기 (정수형 예시)
            binding.tvLivePressureValue.text = String.format("%d", rawPressure.toInt())

            // 4. 원 내부의 물결 애니메이션 수위 실시간 조절 (진행률 0.0 ~ 1.0)
            val currentProgressLevel = elapsed.toFloat() / measureDuration
            binding.waveAnimationView.setWaveProgress(currentProgressLevel)

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

        val baselinePressure = pressureReadings.average().toFloat()
        prefs.baselinePressure = baselinePressure
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
