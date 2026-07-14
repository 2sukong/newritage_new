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
import com.newritage.app.data.SessionDataHolder
import com.newritage.app.data.UserPreferences
import com.newritage.app.ui.main.MainActivity
import com.newritage.app.ui.util.WaveViewNew
import com.newritage.app.util.ThreadColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SessionCompleteActivity : AppCompatActivity() {

    private enum class Screen { RECORD, COMPLETE, THREAD }

    // RECORD (화면 1)
    private lateinit var screenRecord: View
    private lateinit var tvSessionTimeVal: TextView
    private lateinit var tvAvgPressureVal: TextView
    private lateinit var tvMaxMinVal: TextView
    private lateinit var tvDeviationVal: TextView
    private lateinit var etEmotion: EditText
    private lateinit var btnSkip: Button
    private lateinit var btnRecord: Button

    // COMPLETE (화면 2 - 완료 화면)
    private lateinit var screenComplete: View
    private lateinit var waveViewComplete: WaveViewNew
    private lateinit var tvStreakDays: TextView

    // THREAD (화면 3 - 실 제공 화면)
    private lateinit var screenThread: View
    private lateinit var tvThreadDate: TextView
    private lateinit var threadColorView: ImageView    // 두 번째 코드의 ImageView 반영
    private lateinit var tvTensionGauge: TextView
    //private lateinit var tvKeywords: TextView         // 두 번째 코드의 키워드 뷰 반영
    private lateinit var tvAiFeedback: TextView

    // Extras
    private var durationSeconds = 0
    private var avgPressure = 32f
    private var maxPressure = 48f
    private var minPressure = 18f
    private var deviationCount = 0
    private var startTime = ""
    private var endTime = ""

    private var isFirstSessionToday = true // 오늘 첫 세션 여부 판별용
    private var assignedColor: ThreadColors.ThreadColor? = null

    private val dao by lazy { AppDatabase.getInstance(this).sessionDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_complete)

        // Extras 로드
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
        showScreen(Screen.RECORD) // 첫 진입은 감정 기록 화면
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
        //tvKeywords = findViewById(R.id.tvKeywords)
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

        tvAvgPressureVal.text = "%.0fN".format(avgPressure)
        tvMaxMinVal.text = "%.0fN/%.0fN".format(minPressure, maxPressure)
        tvDeviationVal.text = "${deviationCount}회"
    }

    /** 오늘 이미 기록된 세션이 있는지 확인 */
    private fun checkFirstSessionToday() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        lifecycleScope.launch {
            val count = dao.countSessionsByDate(today)
            isFirstSessionToday = (count == 0)
        }
    }

    private fun saveSession(emotion: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val baselineOverall = UserPreferences(this).baselineOverall
        val colorObj = ThreadColors.assignColor(avgPressure, baselineOverall)
        assignedColor = colorObj

        // 키워드 및 랜덤 다채로운 AI 피드백 생성 (첫 번째 코드 알고리즘 이용)
        val keywords = extractKeywords(emotion)
        val feedback = generateAiFeedback(emotion, keywords)

        // 세밀한 센서 데이터 가공 (첫 번째 코드의 고도화된 계산 방식)
        val rawReadings = SessionDataHolder.sensorReadings
        val thumbList = rawReadings.map { it.thumb }.sorted()
        val imList = rawReadings.map { it.indexMiddle }.sorted()
        val palmList = rawReadings.map { it.palm }.sorted()
        val overallList = rawReadings.map { it.overall }.sorted()

        fun List<Float>.median() = if (isEmpty()) 0f else this[size / 2]

        lifecycleScope.launch {
            val countToday = dao.countSessionsByDate(today)

            // 상세 데이터 기반으로 세션 인스턴스 생성
            val session = Session(
                date = today,
                sessionIndex = countToday + 1,
                hasThread = isFirstSessionToday, // 오늘 첫 세션일 때만 실 부여 플래그 true
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSeconds,
                avgPressure = avgPressure,
                maxPressure = maxPressure,
                minPressure = minPressure,
                medianPressure = overallList.median(),

                // 부위별 디테일 저장 (첫 번째 장점 반영)
                thumbAvg = thumbList.let { if (it.isEmpty()) 0f else it.average().toFloat() },
                thumbMin = thumbList.firstOrNull() ?: 0f,
                thumbMax = thumbList.lastOrNull() ?: 0f,
                thumbMedian = thumbList.median(),

                imAvg = imList.let { if (it.isEmpty()) 0f else it.average().toFloat() },
                imMin = imList.firstOrNull() ?: 0f,
                imMax = imList.lastOrNull() ?: 0f,
                imMedian = imList.median(),

                palmAvg = palmList.let { if (it.isEmpty()) 0f else it.average().toFloat() },
                palmMin = palmList.firstOrNull() ?: 0f,
                palmMax = palmList.lastOrNull() ?: 0f,
                palmMedian = palmList.median(),

                emotion = emotion,
                threadColor = colorObj.hex,
                threadColorName = colorObj.nameKr,
                aiFeedback = feedback
            )

            val sessionId = dao.insert(session)

            // 원시 센서 데이터가 있으면 매핑하여 상세 저장
            if (rawReadings.isNotEmpty()) {
                val toSave = rawReadings.map { it.copy(sessionId = sessionId) }
                dao.insertReadings(toSave)
            }
            SessionDataHolder.clear() // 홀더 비우기

            // ─── UX 흐름 전환 시작 ───
            // 1. 감정을 기록하면 무조건 완료 화면(Screen.COMPLETE)을 보여줍니다.
            showScreen(Screen.COMPLETE)
            prepareCompleteScreen()

            // 2. 완료 화면을 2초(2000ms) 동안 강제로 노출하여 연출을 보장합니다.
            delay(2000)

            // 3. 2초 대기 후 오늘 첫 명상 세션이었는지 판별하여 분기 처리합니다.
            if (isFirstSessionToday) {
                // 오늘 첫 세션이면 실 제공 화면(Screen.THREAD)으로 넘어가서 대기합니다.
                showScreen(Screen.THREAD)
                prepareThreadScreen(today, feedback, keywords)
            } else {
                // 오늘 이미 명상을 진행한 상태였다면 바로 홈으로 갑니다.
                goHome()
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
    }

    private fun prepareThreadScreen(today: String, feedback: String, keywords: List<String>) {
        val todayStr = SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault()).format(Date())
        tvThreadDate.text = todayStr

        assignedColor?.let {
            // 두 번째 코드의 이점: 이미지를 드로어블 리소스로 세팅
            threadColorView.setImageResource(it.drawableRes)
            tvTensionGauge.text = "긴장도 ${it.level}/보통"
        }

        // 오늘의 키워드 세팅
        //tvKeywords.text = if (keywords.isEmpty()) "오늘의 키워드: 없음" else "오늘의 키워드: ${keywords.joinToString(", ")}"
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

    /** 첫 번째 코드의 '랜덤 문구 추출형' 다채로운 AI 피드백 생성 알고리즘 */
    private fun generateAiFeedback(emotion: String, keywords: List<String>): String {
        val sentences = mutableListOf<String>()

        // 1. 전반적인 상태 평가 (무작위 다양성 부여)
        val eval = when {
            avgPressure < 35 -> listOf(
                "전체적으로 아주 평온하고 깊은 이완 상태를 유지하셨네요.",
                "몸과 마음이 안정된 상태에서 명상을 잘 이어가셨습니다.",
                "오늘은 긴장보다 편안함이 더 오래 머물렀던 시간이었습니다.",
                "호흡과 리듬이 자연스럽게 이어지며 안정적인 명상을 하셨네요.",
                "전체적으로 차분한 흐름을 유지하며 명상을 마무리하셨습니다.",
                "평소보다 긴장을 잘 내려놓고 편안한 상태에 가까워진 모습이 보입니다."
            ).random()

            avgPressure < 55 -> listOf(
                "적당한 집중력과 안정감 사이에서 균형을 잘 잡으신 명상이었습니다.",
                "크게 흔들리지 않으면서 자신의 호흡에 집중하는 시간이었네요.",
                "긴장과 이완이 자연스럽게 균형을 이루며 명상을 이어가셨습니다.",
                "오늘은 편안함을 찾아가는 과정이 안정적으로 이어졌습니다.",
                "몸의 긴장을 조금씩 풀어가며 차분하게 시간을 보내셨네요.",
                "무리하지 않고 자신의 속도에 맞춰 명상을 이어간 점이 인상적입니다."
            ).random()

            else -> listOf(
                "오늘 평소보다 조금 더 긴장된 상태로 명상을 시작하셨던 것 같아요.",
                "몸에 긴장이 남아 있었지만 끝까지 명상을 이어가셨습니다.",
                "바쁜 하루의 흔적이 몸에 남아 있었던 하루였던 것 같습니다.",
                "오늘은 긴장이 쉽게 풀리지는 않았지만 충분히 의미 있는 시간이었습니다.",
                "처음에는 긴장감이 느껴졌지만 잠시라도 자신을 위한 시간을 만들어 주셨네요.",
                "완전히 편안해지지는 않았더라도 잠시 멈춰 쉬어간 것만으로도 좋은 선택이었습니다."
            ).random()
        }
        sentences.add(eval)

        // 2. 긴장도 변화 및 특이점
        val trend = when {
            maxPressure > 70 -> listOf(
                "명상 도중 잠시 긴장도가 높게 올라간 순간이 있었지만, 다시 호흡을 가다듬고 돌아오려 애쓰셨습니다.",
                "중간에 긴장이 크게 올라갔지만 다시 집중을 되찾으려는 흐름이 보였습니다.",
                "잠깐의 긴장에도 명상을 이어간 점이 인상적입니다.",
                "긴장이 높아진 순간이 있었지만 포기하지 않고 끝까지 함께해 주셨네요.",
                "몸이 순간적으로 반응했지만 다시 차분함을 찾아가는 모습이 보였습니다.",
                "높은 긴장이 나타났지만 그 순간을 지나 다시 호흡에 집중하셨습니다."
            ).random()

            avgPressure - minPressure > 10 -> listOf(
                "시작할 때보다 긴장도가 서서히 낮아지며 몸과 마음이 한결 가벼워지는 흐름을 보여주셨어요.",
                "명상이 진행될수록 점차 안정감을 찾아가는 모습이 확인됩니다.",
                "처음보다 훨씬 편안한 상태로 마무리하셨네요.",
                "호흡에 집중할수록 몸의 긴장이 자연스럽게 풀리는 흐름이었습니다.",
                "시간이 흐르면서 몸이 차분하게 이완되는 모습이 느껴졌습니다.",
                "명상이 끝날 무렵에는 한결 안정된 상태에 가까워졌습니다."
            ).random()

            else -> listOf(
                "안정적인 긴장도 수치를 꾸준히 유지하며 고요하게 머무르셨습니다.",
                "큰 변화 없이 편안한 흐름을 유지한 명상이었습니다.",
                "호흡과 긴장도가 안정적으로 이어진 시간이었습니다.",
                "급격한 변화 없이 자신의 리듬을 잘 유지하셨네요.",
                "차분한 상태를 꾸준히 유지하며 명상을 마무리하셨습니다.",
                "일정한 호흡과 함께 안정적인 흐름이 이어졌습니다."
            ).random()
        }
        sentences.add(trend)

        // 3. 사용자 기록 공감
        val keywordFeedback = when {
            keywords.isNotEmpty() -> listOf(
                "오늘 기록하신 경험이 명상을 통해 당신의 내면에 긍정적인 파동을 남겼기를 바랍니다.",
                "기록에 담긴 하루를 천천히 돌아볼 수 있는 시간이 되었기를 바랍니다.",
                "오늘 남겨주신 {keywords}이라는 기록이 스스로를 이해하는 작은 단서가 되었으면 좋겠습니다.",
                "기록 속 {keywords}처럼 오늘 하루를 있는 그대로 바라본 시간이었습니다.",
                "오늘의 {keywords}이 앞으로의 하루를 조금 더 편안하게 만드는 힘이 되기를 바랍니다.",
                "짧은 기록이지만 {keywords}에는 오늘의 마음이 잘 담겨 있었습니다."
            ).random().replace("{keywords}", keywords.joinToString(", "))

            emotion.isNotEmpty() -> listOf(
                "오늘의 감정을 글로 남기며 스스로를 돌아보는 모습이 참 아름답습니다.",
                "감정을 기록하는 것만으로도 자신의 하루를 정리하는 데 큰 도움이 됩니다.",
                "오늘의 마음을 솔직하게 남겨주셔서 감사합니다.",
                "감정을 표현하는 시간이 스스로를 이해하는 작은 시작이 될 수 있습니다.",
                "오늘의 감정을 천천히 들여다본 것만으로도 충분히 의미 있는 시간이었습니다.",
                "자신의 마음을 기록하는 습관이 차분한 하루를 만드는 데 도움이 될 거예요."
            ).random()

            else -> listOf(
                "${durationSeconds / 60}분이라는 소중한 시간을 오롯이 자신에게 선물하신 점을 칭찬해 드리고 싶어요.",
                "기록이 없어도 오늘 명상에 집중한 시간은 충분히 의미가 있습니다.",
                "잠시라도 자신을 위해 시간을 내어준 것만으로도 좋은 하루였습니다.",
                "오늘의 명상이 몸과 마음을 쉬게 하는 시간이 되었기를 바랍니다.",
                "짧은 시간이었지만 자신에게 집중하는 시간을 만들어 주셨네요.",
                "하루 중 잠시 멈춰 호흡을 바라본 시간이 내일에도 좋은 영향을 줄 것입니다."
            ).random().replace("{duration}", "${durationSeconds / 60}")
        }
        sentences.add(keywordFeedback)

        // 4. 마무리 응원
        val ending = listOf(
            "오늘의 평온함이 일상까지 이어지길 바라며, 내일도 이 자리에서 당신을 기다리고 있겠습니다.",
            "오늘의 여유가 하루를 조금 더 편안하게 만들어 주길 바랍니다.",
            "내일도 지금처럼 잠시 멈춰 자신을 돌보는 시간을 가져보세요.",
            "작은 명상의 시간이 쌓여 더 편안한 일상이 만들어질 거예요.",
            "오늘도 수고 많으셨습니다. 편안한 하루 보내세요.",
            "다음 명상에서도 지금처럼 자신의 호흡에 천천히 집중해 보세요."
        ).random()
        sentences.add(ending)

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