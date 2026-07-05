package com.newritage.app.ui.measurement

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.Session
import com.newritage.app.data.SessionDataHolder
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivitySessionCompleteBinding
import com.newritage.app.stats.StatsCalculator
import com.newritage.app.ui.main.MainActivity
import com.newritage.app.util.ThreadColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionCompleteActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionCompleteBinding
    private lateinit var prefs: UserPreferences

    private var assignedColor: ThreadColors.ThreadColor? = null

    // 세션 데이터
    private var durationSeconds = 0
    private var avgPressure = 0f
    private var maxPressure = 0f
    private var minPressure = 0f

    enum class Screen { COMPLETE, RECORD, THREAD }

    private val dao by lazy { AppDatabase.getInstance(this).sessionDao() }

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

        showScreen(Screen.RECORD)
        populateRecord()
        setupButtons()
    }

    private fun setupButtons() {
        // 저장 → 하루 첫 세션이면 실 제공 화면, 아니면 완료 화면
        binding.btnSaveRecord.setOnClickListener {
            saveSession()
        }

        // 완료(스트릭) 화면 → 메인
        binding.btnGoRecord.setOnClickListener { goHome() }

        // 실 제공 → 메인
        binding.btnGoMain.setOnClickListener { goHome() }
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

        val readings = SessionDataHolder.sensorReadings
        val vibrationCount = SessionDataHolder.vibrationCount
        val stats = StatsCalculator().calculateDailyReport(readings)
        val overallStats = stats["overall"]
        val thumbStats = stats["thumb"]
        val imStats = stats["indexMiddle"]
        val palmStats = stats["palm"]

        val colorObj = ThreadColors.assignColor(avgPressure, prefs.baselineOverall)
        val keywords = extractKeywords(emotion)
        val feedback = generateAiFeedback(emotion, keywords)

        lifecycleScope.launch {
            val countToday = dao.countSessionsByDate(today)
            val isFirstSession = countToday == 0

            val session = Session(
                date = today,
                sessionIndex = countToday + 1,
                hasThread = isFirstSession,
                durationSeconds = durationSeconds,
                avgPressure = avgPressure,
                maxPressure = maxPressure,
                minPressure = minPressure,
                medianPressure = overallStats?.median ?: 0f,
                thumbAvg = thumbStats?.avg ?: 0f,
                thumbMin = thumbStats?.min ?: 0f,
                thumbMax = thumbStats?.max ?: 0f,
                thumbMedian = thumbStats?.median ?: 0f,
                imAvg = imStats?.avg ?: 0f,
                imMin = imStats?.min ?: 0f,
                imMax = imStats?.max ?: 0f,
                imMedian = imStats?.median ?: 0f,
                palmAvg = palmStats?.avg ?: 0f,
                palmMin = palmStats?.min ?: 0f,
                palmMax = palmStats?.max ?: 0f,
                palmMedian = palmStats?.median ?: 0f,
                vibrationCount = vibrationCount,
                emotion = emotion,
                threadColor = if (isFirstSession) colorObj.hex else "",
                threadColorName = if (isFirstSession) colorObj.nameKr else "",
                aiFeedback = feedback
            )

            val sessionId = dao.insert(session)
            prefs.lastSessionId = sessionId

            if (readings.isNotEmpty()) {
                dao.insertReadings(readings.map { it.copy(sessionId = sessionId) })
            }
            SessionDataHolder.clear()

            runOnUiThread {
                if (isFirstSession) {
                    assignedColor = colorObj
                    showScreen(Screen.THREAD)
                    showThreadProvide(keywords, feedback)
                } else {
                    showScreen(Screen.COMPLETE)
                    showStreak()
                }
            }
        }
    }

    private fun showStreak() {
        lifecycleScope.launch {
            val totalDays = dao.getTotalActiveDays()
            binding.tvStreakDays.text = getString(R.string.streak_days_format, totalDays)
        }
    }

    private fun showThreadProvide(keywords: List<String>, feedback: String) {
        val color = assignedColor
        if (color != null) {
            try {
                binding.threadColorView.setBackgroundColor(Color.parseColor(color.hex))
            } catch (e: IllegalArgumentException) {
                binding.threadColorView.setBackgroundColor(Color.parseColor("#8B9E7B"))
            }
            binding.tvThreadColorName.text = color.nameKr
        }
        binding.tvKeywords.text =
            if (keywords.isEmpty()) "" else "오늘의 키워드: ${keywords.joinToString(", ")}"
        binding.tvAiFeedback.text = feedback
    }

    private fun extractKeywords(text: String): List<String> {
        val categories = mapOf(
            "긍정" to listOf("평화", "편안", "안정", "차분", "행복", "감사", "기쁨", "만족", "여유", "힐링", "좋았다", "좋음", "개운하다", "개운했다", "상쾌하다", "상쾌했다", "편해졌다", "편했다", "가벼웠다", "한결 나아졌다", "괜찮았다", "즐거웠다", "기분 좋았다", "마음이 놓였다"),
            "긴장" to listOf("불안", "걱정", "스트레스", "초조", "긴장", "압박", "피곤", "예민", "답답하다", "답답했다", "힘들었다", "지쳤다", "무거웠다", "복잡했다", "산만했다", "부담됐다", "떨렸다", "불편했다", "불안정했다"),
            "집중" to listOf("집중", "몰입", "호흡", "생각", "잡생각", "명상", "의식", "집중됐다", "집중이 안됐다", "집중이 어려웠다", "몰입됐다", "호흡에 집중", "호흡이 편안했다", "잡념", "멍했다", "멍해졌다", "생각이 많았다", "생각이 줄었다"),
            "감정" to listOf("슬픔", "우울", "외로움", "화남", "짜증", "눈물", "속상했다", "답답했다", "허무했다", "공허했다", "서운했다", "후회", "후련했다", "울컥했다", "감동", "설렜다"),
            "성장" to listOf("노력", "도전", "변화", "회복", "성장", "꾸준함", "익숙해졌다", "나아졌다", "발전했다", "좋아졌다", "버텼다", "해냈다", "성공했다", "다시 해보고 싶다", "계속하고 싶다", "꾸준히 하고 싶다")
        )

        val extracted = mutableSetOf<String>()
        for ((_, words) in categories) {
            for (word in words) {
                if (text.contains(word)) {
                    extracted.add(word)
                    if (extracted.size >= 3) return extracted.toList()
                }
            }
        }
        return extracted.toList()
    }

    private fun generateAiFeedback(emotion: String, keywords: List<String>): String {
        val sentences = mutableListOf<String>()

        val eval = if (avgPressure < 35) "전체적으로 아주 평온하고 깊은 이완 상태를 유지하셨네요."
        else if (avgPressure < 55) "적당한 집중력과 안정감 사이에서 균형을 잘 잡으신 명상이었습니다."
        else "오늘 평소보다 조금 더 긴장된 상태로 명상을 시작하셨던 것 같아요."
        sentences.add(eval)

        val trend = if (maxPressure > 70) {
            "명상 도중 잠시 긴장도가 높게 올라간 순간이 있었지만, 다시 호흡을 가다듬고 돌아오려 애쓰신 과정이 소중합니다."
        } else if (avgPressure - minPressure > 10) {
            "시작할 때보다 긴장도가 서서히 낮아지며 몸과 마음이 한결 가벼워지는 흐름을 보여주셨어요."
        } else {
            "안정적인 긴장도 수치를 꾸준히 유지하며 고요하게 머무르셨습니다."
        }
        sentences.add(trend)

        val keywordFeedback = if (keywords.isNotEmpty()) {
            "오늘 기록하신 '${keywords.joinToString(", ")}'의 경험이 명상을 통해 당신의 내면에 긍정적인 파동을 남겼기를 바랍니다."
        } else if (emotion.isNotEmpty()) {
            "오늘의 감정을 글로 남기며 스스로를 돌아보는 모습이 참 아름답습니다."
        } else {
            "${durationSeconds / 60}분이라는 소중한 시간을 오롯이 자신에게 선물하신 점을 칭찬해 드리고 싶어요."
        }
        sentences.add(keywordFeedback)

        sentences.add("오늘의 평온함이 일상까지 이어지길 바라며, 내일도 이 자리에서 당신을 기다리고 있겠습니다.")

        return sentences.joinToString("\n\n")
    }

    private fun showScreen(screen: Screen) {
        binding.layoutComplete.visibility = if (screen == Screen.COMPLETE) View.VISIBLE else View.GONE
        binding.layoutRecord.visibility = if (screen == Screen.RECORD) View.VISIBLE else View.GONE
        binding.layoutThread.visibility = if (screen == Screen.THREAD) View.VISIBLE else View.GONE
    }

    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("NAVIGATE_TO_HOME", true)
        startActivity(intent)
        finish()
    }
}
