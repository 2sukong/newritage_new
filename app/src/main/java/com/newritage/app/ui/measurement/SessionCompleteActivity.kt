package com.newritage.app.ui.measurement

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.Session
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivitySessionCompleteBinding
import com.newritage.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class SessionCompleteActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionCompleteBinding
    private lateinit var prefs: UserPreferences

    private var savedSessionId: Long = -1L
    private var generatedColor: String = "#8B9E7B"

    // 세션 데이터
    private var durationSeconds = 0
    private var avgPressure = 0f
    private var maxPressure = 0f
    private var minPressure = 0f

    enum class Screen { COMPLETE, RECORD, THREAD }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)

        // Intent로 전달받은 세션 데이터
        durationSeconds = intent.getIntExtra("duration_seconds", 0)
        avgPressure = intent.getFloatExtra("avg_pressure", 0f)
        maxPressure = intent.getFloatExtra("max_pressure", 0f)
        minPressure = intent.getFloatExtra("min_pressure", 0f)

        // 실 색상 생성 (평균 압력 기반 색상 매핑)
        generatedColor = generateThreadColor(avgPressure)

        showScreen(Screen.COMPLETE)
        setupButtons()
    }

    private fun setupButtons() {
        // 완료 → 기록 화면
        binding.btnGoRecord.setOnClickListener {
            showScreen(Screen.RECORD)
            populateRecord()
        }

        // 기록 저장 → 실 제공 화면
        binding.btnSaveRecord.setOnClickListener {
            saveSession()
        }

        // 실 제공 → 메인
        binding.btnGoMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun populateRecord() {
        val min = durationSeconds / 60
        val sec = durationSeconds % 60
        binding.tvRecordMedTime.text = String.format("%02d:%02d", min, sec)
        binding.tvRecordAvgPressure.text = String.format("%.1f kPa", avgPressure)
        binding.tvRecordMaxPressure.text = String.format("%.1f kPa", maxPressure)
        binding.tvRecordMinPressure.text = String.format("%.1f kPa", minPressure)
    }

    private fun saveSession() {
        val emotion = binding.etEmotion.text?.toString()?.trim() ?: ""
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val session = Session(
            date = today,
            durationSeconds = durationSeconds,
            avgPressure = avgPressure,
            maxPressure = maxPressure,
            minPressure = minPressure,
            emotion = emotion,
            threadColor = generatedColor
        )

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@SessionCompleteActivity)
            val id = db.sessionDao().insert(session)
            savedSessionId = id
            prefs.lastSessionId = id

            runOnUiThread {
                showScreen(Screen.THREAD)
                showThreadProvide()
            }
        }
    }

    private fun showThreadProvide() {
        try {
            binding.threadColorView.setBackgroundColor(Color.parseColor(generatedColor))
        } catch (e: Exception) {
            binding.threadColorView.setBackgroundColor(Color.parseColor("#8B9E7B"))
        }
        binding.tvThreadColorName.text = colorName(generatedColor)
    }

    /** 평균 압력 기반 실 색상 생성 (플레이스홀더 로직) */
    private fun generateThreadColor(avgPressure: Float): String {
        val colors = listOf(
            "#A8C5A0", "#B5C9A5", "#8FAF7F", "#C5B5A0",
            "#A0B5C5", "#C5A0B5", "#B5A0C5", "#A0C5B5",
            "#C5C5A0", "#A0A0C5"
        )
        val index = (avgPressure / 8).toInt().coerceIn(0, colors.size - 1)
        return colors[index]
    }

    private fun colorName(hex: String): String {
        return when (hex) {
            "#A8C5A0" -> "연두빛 평온"
            "#B5C9A5" -> "풀빛 고요"
            "#8FAF7F" -> "숲의 정취"
            "#C5B5A0" -> "따뜻한 모래"
            "#A0B5C5" -> "하늘빛 여유"
            "#C5A0B5" -> "라벤더 안정"
            "#B5A0C5" -> "보라빛 사색"
            "#A0C5B5" -> "민트 청량"
            "#C5C5A0" -> "황록빛 집중"
            "#A0A0C5" -> "청보라 명상"
            else -> "나만의 색"
        }
    }

    private fun showScreen(screen: Screen) {
        binding.layoutComplete.visibility = if (screen == Screen.COMPLETE) View.VISIBLE else View.GONE
        binding.layoutRecord.visibility = if (screen == Screen.RECORD) View.VISIBLE else View.GONE
        binding.layoutThread.visibility = if (screen == Screen.THREAD) View.VISIBLE else View.GONE
    }
}
