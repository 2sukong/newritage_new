package com.newritage.app.ui.measurement

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.Session
import com.newritage.app.data.UserPreferences
import com.newritage.app.ui.main.MainActivity
import com.newritage.app.ui.util.WaveViewNew
import com.newritage.app.util.ThreadColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SessionCompleteActivity : AppCompatActivity() {

    private enum class Screen { RECORD, COMPLETE, THREAD }

    // RECORD
    private lateinit var screenRecord: View
    private lateinit var tvSessionTimeVal: TextView
    private lateinit var tvAvgPressureVal: TextView
    private lateinit var tvMaxMinVal: TextView
    private lateinit var tvDeviationVal: TextView
    private lateinit var etEmotion: EditText
    private lateinit var btnSkip: Button
    private lateinit var btnRecord: Button

    // COMPLETE
    private lateinit var screenComplete: View
    private lateinit var waveViewComplete: WaveViewNew
    private lateinit var tvStreakDays: TextView

    // THREAD
    private lateinit var screenThread: View
    private lateinit var tvThreadDate: TextView
    private lateinit var threadColorView: View
    private lateinit var tvTensionGauge: TextView
    private lateinit var tvKeywords: TextView
    private lateinit var tvAiFeedback: TextView

    // Extras
    private var durationSeconds = 0
    private var avgPressure = 32f
    private var maxPressure = 48f
    private var minPressure = 18f
    private var deviationCount = 0
    private var startTime = ""
    private var endTime = ""

    private var isFirstSession = true
    private var assignedColor: ThreadColors.ThreadColor? = null

    private val dao by lazy { AppDatabase.getInstance(this).sessionDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_complete)

        // Extras
        durationSeconds = intent.getIntExtra("duration_seconds", 0)
        avgPressure     = intent.getFloatExtra("avg_pressure", 32f)
        maxPressure     = intent.getFloatExtra("max_pressure", 48f)
        minPressure     = intent.getFloatExtra("min_pressure", 18f)
        deviationCount  = intent.getIntExtra("deviation_count", 0)
        startTime       = intent.getStringExtra("start_time") ?: ""
        endTime         = intent.getStringExtra("end_time") ?: ""

        initViews()
        setupListeners()
        loadStats()
        checkFirstSessionToday()
        showScreen(Screen.RECORD)
    }

    private fun initViews() {
        screenRecord = findViewById(R.id.screenRecord)
        tvSessionTimeVal = findViewById(R.id.tvSessionTimeVal)
        tvAvgPressureVal = findViewById(R.id.tvAvgPressureVal)
        tvMaxMinVal = findViewById(R.id.tvMaxMinVal)
        tvDeviationVal = findViewById(R.id.tvDeviationVal)
        etEmotion = findViewById(R.id.etEmotion)
        btnSkip = findViewById(R.id.btnSkip)
        btnRecord = findViewById(R.id.btnRecord)

        screenComplete = findViewById(R.id.screenComplete)
        waveViewComplete = findViewById(R.id.waveViewComplete)
        tvStreakDays = findViewById(R.id.tvStreakDays)

        screenThread = findViewById(R.id.screenThread)
        tvThreadDate = findViewById(R.id.tvThreadDate)
        threadColorView = findViewById(R.id.threadColorView)
        tvTensionGauge = findViewById(R.id.tvTensionGauge)
        tvKeywords = findViewById(R.id.tvKeywords)
        tvAiFeedback = findViewById(R.id.tvAiFeedback)

        findViewById<ImageButton>(R.id.btnBackRecord).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvHeaderHome).setOnClickListener { goHome() }
    }

    private fun setupListeners() {
        btnSkip.setOnClickListener { saveSession("") }
        btnRecord.setOnClickListener { saveSession(etEmotion.text.toString()) }
    }

    private fun loadStats() {
        tvSessionTimeVal.text = if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
            "$startTime-$endTime"
        } else {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            "%02d:%02d".format(minutes, seconds)
        }

        // Using "N" as per the prototype image
        tvAvgPressureVal.text = "%.0fN".format(avgPressure)
        tvMaxMinVal.text = "%.0fN/%.0fN".format(minPressure, maxPressure)
        tvDeviationVal.text = "${deviationCount}회"
    }

    private fun checkFirstSessionToday() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        lifecycleScope.launch {
            val count = dao.countSessionsByDate(today)
            isFirstSession = (count == 0)
        }
    }

    private fun saveSession(emotion: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val baselineOverall = UserPreferences(this).baselineOverall
        val colorObj = ThreadColors.assignColor(avgPressure, baselineOverall)
        assignedColor = colorObj

        // 키워드 추출 및 피드백 생성
        val keywords = extractKeywords(emotion)
        val feedback = generateAiFeedback(emotion, keywords)

        lifecycleScope.launch {
            // Get current session index
            val countToday = dao.countSessionsByDate(today)

            val session = Session(
                date = today,
                sessionIndex = countToday + 1,
                hasThread = isFirstSession,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSeconds,
                avgPressure = avgPressure,
                maxPressure = maxPressure,
                minPressure = minPressure,
                emotion = emotion,
                threadColor = colorObj.hex,
                threadColorName = colorObj.nameKr,
                aiFeedback = feedback
            )

            dao.insert(session)

            if (isFirstSession) {
                showScreen(Screen.THREAD)
                prepareThreadScreen(today, feedback, keywords)
            } else {
                showScreen(Screen.COMPLETE)
                prepareCompleteScreen()
            }
        }
    }

    private fun showScreen(screen: Screen) {
        screenRecord.visibility = if (screen == Screen.RECORD) View.VISIBLE else View.GONE
        screenComplete.visibility = if (screen == Screen.COMPLETE) View.VISIBLE else View.GONE
        screenThread.visibility = if (screen == Screen.THREAD) View.VISIBLE else View.GONE
    }

    private fun prepareCompleteScreen() {
        waveViewComplete.setPressure(avgPressure)
        lifecycleScope.launch {
            val totalDays = dao.getTotalActiveDays()
            tvStreakDays.text = getString(R.string.streak_days_format, totalDays)
        }
        // Auto finish after 2.5 seconds
        screenComplete.postDelayed({ goHome() }, 2500)
    }

    private fun prepareThreadScreen(today: String, feedback: String, keywords: List<String>) {
        val todayStr = SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault()).format(Date())
        tvThreadDate.text = todayStr
        assignedColor?.let {
            threadColorView.setBackgroundColor(Color.parseColor(it.hex))
            tvTensionGauge.text = "긴장도 ${it.level}/보통"
        }

        tvKeywords.text = if (keywords.isEmpty()) "오늘의 키워드: 없음" else "오늘의 키워드: ${keywords.joinToString(", ")}"
        tvAiFeedback.text = feedback
    }

    private fun extractKeywords(text: String): List<String> {
        val categories = mapOf(
            "긍정" to listOf("평화", "편안", "안정", "차분", "행복", "감사", "기쁨", "만족", "여유", "힐링", "좋았다", "좋음", "개운하다", "개운했다", "상쾌하다", "상쾌했다", "편해졌다", "편했다", "가벼웠다", "한결 나아졌다", "괜찮았다", "즐거웠다", "기분 좋았다", "마음이 놓였다"),
            "긴장" to listOf("불안", "걱정", "스트레스", "초조", "긴장", "압박", "피곤", "예민", "답답하다", "답답했다", "힘들었다", "지쳤다", "무거웠다", "복잡했다", "산만했다", "부담됐다", "떨렸다", "불편했다", "불안정했다"),
            "집중" to listOf("집중", "몰입", "호흡", "생각", "잡생각", "명상", "의식", "집중됐다", "집중이 안됐다", "집중이 어려웠다", "몰입됐다", "호흡에 집중", "호흡이 편안했다", "잡념", "멍했다", "멍해졌다", "생각이 많았다", "생각이 줄었다"),
            "감정" to listOf("슬픔", "우울", "외로움", "화남", "짜증", "눈물", "속상했다", "답답했다", "허무했다", "공허했다", "서운했다", "후회", "후련했다", "울컥했다", "감동", "설렜다"),
            "성장" to listOf("노력", "도전", "변화", "회복", "성장", "꾸준함", "익숙해졌다", "나아졌다", "발전했다", "좋아졌다", "버텼다", "해냈다", "성공했다", "다시 해보고 싶다", "계속하고 싶다", "꾸준히 하고 싶다")
        )

        val extracted = mutableSetOf<String>() // 중복 방지
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

        // 1. 전반적인 상태 평가
        val eval = if (avgPressure < 35) "전체적으로 아주 평온하고 깊은 이완 상태를 유지하셨네요."
        else if (avgPressure < 55) "적당한 집중력과 안정감 사이에서 균형을 잘 잡으신 명상이었습니다."
        else "오늘 평소보다 조금 더 긴장된 상태로 명상을 시작하셨던 것 같아요."
        sentences.add(eval)

        // 2. 긴장도 변화 및 특이점 (최고 긴장도 언급)
        val trend = if (maxPressure > 70) {
            "명상 도중 잠시 긴장도가 높게 올라간 순간이 있었지만, 다시 호흡을 가다듬고 돌아오려 애쓰신 과정이 소중합니다."
        } else if (avgPressure - minPressure > 10) {
            "시작할 때보다 긴장도가 서서히 낮아지며 몸과 마음이 한결 가벼워지는 흐름을 보여주셨어요."
        } else {
            "안정적인 긴장도 수치를 꾸준히 유지하며 고요하게 머무르셨습니다."
        }
        sentences.add(trend)

        // 3. 사용자 기록 공감 (키워드 활용)
        val keywordFeedback = if (keywords.isNotEmpty()) {
            "오늘 기록하신 '${keywords.joinToString(", ")}'의 경험이 명상을 통해 당신의 내면에 긍정적인 파동을 남겼기를 바랍니다."
        } else if (emotion.isNotEmpty()) {
            "오늘의 감정을 글로 남기며 스스로를 돌아보는 모습이 참 아름답습니다."
        } else {
            "${durationSeconds / 60}분이라는 소중한 시간을 오롯이 자신에게 선물하신 점을 칭찬해 드리고 싶어요."
        }
        sentences.add(keywordFeedback)

        // 4. 마무리 응원
        sentences.add("오늘의 평온함이 일상까지 이어지길 바라며, 내일도 이 자리에서 당신을 기다리고 있겠습니다.")

        // 출력 형식: 문장 사이 한 줄씩 띄움
        return sentences.joinToString("\n\n")
    }

    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("NAVIGATE_TO_HOME", true)
        startActivity(intent)
        finish()
    }
}
