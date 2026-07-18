package com.newritage.app.ui.measurement

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.GeminiRepository
import com.newritage.app.data.KnotType
import com.newritage.app.data.Session
import com.newritage.app.data.SessionDataHolder
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivitySessionCompleteBinding
import com.newritage.app.stats.StatsCalculator
import com.newritage.app.ui.main.MainActivity
import com.newritage.app.ui.main.knot.recommend.DiaryEntry
import com.newritage.app.ui.main.knot.recommend.RecommendationEngine
import com.newritage.app.ui.util.WaveStyle
import com.newritage.app.util.PressureDisplay
import com.newritage.app.util.ThreadColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class SessionCompleteActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionCompleteBinding
    private lateinit var prefs: UserPreferences

    private var assignedColor: ThreadColors.ThreadColor? = null

    // 세션 데이터
    private var durationSeconds = 0
    private var avgPressure = 0f
    private var maxPressure = 0f
    private var minPressure = 0f
    private var sessionStartMillis = 0L
    private var sessionEndMillis = 0L

    enum class Screen { RECORD, SAVED, THREAD }

    private val dao by lazy { AppDatabase.getInstance(this).sessionDao() }
    private val geminiRepository by lazy { GeminiRepository(dao) }

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
        sessionStartMillis = intent.getLongExtra("session_start_millis", 0L)
        sessionEndMillis = intent.getLongExtra("session_end_millis", 0L)

        showScreen(Screen.RECORD)
        populateRecord()
        setupButtons()
    }

    private fun setupButtons() {
        binding.btnBackRecord.setOnClickListener { finish() }
        binding.btnSaveRecord.setOnClickListener { saveSession(skipEmotion = false) }
        binding.btnSkipRecord.setOnClickListener { saveSession(skipEmotion = true) }

        // 실 제공 → 메인
        binding.btnGoMain.setOnClickListener { goHome() }
    }

    private fun populateRecord() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.tvRecordMedTime.text = if (sessionStartMillis > 0 && sessionEndMillis > 0) {
            getString(
                R.string.review_time_range_format,
                timeFormat.format(Date(sessionStartMillis)),
                timeFormat.format(Date(sessionEndMillis))
            )
        } else {
            String.format("%02d:%02d", durationSeconds / 60, durationSeconds % 60)
        }

        val avgN = PressureDisplay.toDisplayValue(avgPressure).roundToInt()
        val maxN = PressureDisplay.toDisplayValue(maxPressure).roundToInt()
        val minN = PressureDisplay.toDisplayValue(minPressure).roundToInt()
        binding.tvRecordAvgPressure.text = getString(R.string.review_tension_value_format, avgN)
        binding.tvRecordMinMaxPressure.text = getString(R.string.review_minmax_format, maxN, minN)
        binding.tvRecordDeviationCount.text =
            getString(R.string.review_deviation_count_format, SessionDataHolder.vibrationCount)
    }

    private fun saveSession(skipEmotion: Boolean) {
        val emotion = if (skipEmotion) "" else (binding.etEmotion.text?.toString()?.trim() ?: "")
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

        // 안정 상태 비율(%) — GeminiRepository의 STABLE_THRESHOLD(50kPa)와 동일한 기준으로,
        // 원시 표본 중 압력이 50 이하로 유지된 비율을 그대로 계산한다(팀원 코드의 판정 기준 그대로 유지).
        val stableRatio = if (readings.isNotEmpty()) {
            readings.count { it.overall <= 50f }.toFloat() / readings.size * 100f
        } else {
            100f
        }

        lifecycleScope.launch {
            val countToday = dao.countSessionsByDate(today)
            val isFirstSession = countToday == 0

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val baseSession = Session(
                date = today,
                sessionIndex = countToday + 1,
                hasThread = isFirstSession,
                startTime = if (sessionStartMillis > 0) timeFormat.format(Date(sessionStartMillis)) else "",
                endTime = if (sessionEndMillis > 0) timeFormat.format(Date(sessionEndMillis)) else "",
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
                threadColorName = if (isFirstSession) colorObj.nameKr else ""
            )

            // Gemini API를 우선 시도하고, 실패(키 누락/네트워크 오류 등) 시에만 로컬 템플릿으로 폴백한다.
            val feedback = geminiRepository.generateDailyFeedback(baseSession, stableRatio, readings)
                ?: generateAiFeedback(emotion, keywords)
            val session = baseSession.copy(aiFeedback = feedback)

            val sessionId = dao.insert(session)
            prefs.lastSessionId = sessionId

            if (readings.isNotEmpty()) {
                dao.insertReadings(readings.map { it.copy(sessionId = sessionId) })
            }
            SessionDataHolder.clear()

            runOnUiThread {
                showSavedScreen(isFirstSession, colorObj, vibrationCount)
            }
        }
    }

    /**
     * 저장 직후 몇 초간 보여주는 애니메이션 화면. 컵에 물이 차오르듯 WaveView를 0에서부터
     * 채우고, 그 뒤로는 사용자 조작 없이 자동으로 다음 화면(실 제공 또는 메인)으로 넘어간다.
     */
    private fun showSavedScreen(
        isFirstSession: Boolean,
        colorObj: ThreadColors.ThreadColor,
        vibrationCount: Int
    ) {
        showScreen(Screen.SAVED)

        binding.waveViewSaved.setWaveStyle(WaveStyle.COMPLETE)
        binding.waveViewSaved.setPressure(0f)
        binding.waveViewSaved.startWave()

        ValueAnimator.ofFloat(0f, SAVED_FILL_TARGET).apply {
            duration = SAVED_FILL_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { binding.waveViewSaved.setPressure(it.animatedValue as Float) }
            start()
        }

        lifecycleScope.launch {
            val totalDays = dao.getTotalActiveDays()
            binding.tvStreakDays.text = getString(R.string.streak_record_format, totalDays)
        }

        lifecycleScope.launch {
            delay(SAVED_SCREEN_DURATION_MS)
            if (isFirstSession) {
                assignedColor = colorObj
                showScreen(Screen.THREAD)
                showThreadProvide(vibrationCount)
            } else {
                goHome()
            }
        }
    }

    private suspend fun showThreadProvide(vibrationCount: Int) {
        val color = assignedColor
        if (color != null) {
            // 단색 대신 실 색상별 사진(drawableRes)을 프레임 안에 보여준다(master와 동일).
            binding.threadColorView.setImageResource(color.drawableRes)
            binding.tvThreadColorName.text = color.nameKr
        }

        binding.tvThreadDate.text = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

        val tensionBadgeValue = ((avgPressure / RAW_PRESSURE_MAX) * TENSION_BADGE_MAX)
            .roundToInt().coerceIn(0, TENSION_BADGE_MAX.toInt())
        binding.tvTensionBadge.text = getString(R.string.thread_tension_badge_format, tensionBadgeValue)

        val (para1, para2) = buildThreadFeedback(vibrationCount)
        binding.tvFeedbackPara1.text = para1
        binding.tvFeedbackPara2.text = para2

        // 매듭 보관함(KnotStorageFragment)과 동일한 기준: 이번 달 일기(emotion)를 감정 분석해
        // "이달의 매듭"을 추천하고, 그 매듭을 바로 팝업으로 보여준다.
        val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val diaryEntries = dao.getSessionsByMonth(yearMonth)
            .filter { it.emotion.isNotBlank() }
            .map { DiaryEntry(date = LocalDate.parse(it.date), content = it.emotion) }
        val recommendedKnot = RecommendationEngine.recommendKnot(diaryEntries)
        val knotType = KnotType.fromRecommendationId(recommendedKnot.id)
        KnotCreatedDialog(this, knotType) { navigateToKnotTab() }.show()
    }

    private fun navigateToKnotTab() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(MainActivity.EXTRA_NAVIGATE_TO_KNOT, true)
        startActivity(intent)
        finish()
    }

    /**
     * baseline 대비 평균 긴장도 수준 + 안정 상태 이탈 빈도, 두 문장을 만든다. 긴장도 수준 경계는
     * ThreadColors.assignColor()가 실 색상을 나눌 때 쓰는 것과 같은 비율(1.1/1.3)을 재사용해
     * "이 색 실을 받은 이유"와 "오늘의 피드백"이 서로 어긋나지 않게 했다.
     */
    private fun buildThreadFeedback(vibrationCount: Int): Pair<String, String> {
        val ratio = avgPressure / prefs.baselineOverall.coerceAtLeast(1f)
        val levelPara = when {
            ratio < 1.1f -> "평소보다 여유로운 긴장도가 측정되었어요.\n편안한 상태를 잘 유지하셨습니다."
            ratio < 1.3f -> "평소와 비슷한 안정적인 긴장도가 측정되었어요.\n꾸준한 리듬을 잘 지키고 계세요."
            else -> "평소보다 비교적 높은 긴장도가 측정되었어요.\n천천히 호흡하면 몸의 긴장을 이완시킬 수 있어요."
        }

        // TODO: 이탈 빈도 구간(0 / 1~4 / 5+) 기준값은 실기 데이터 쌓이는 대로 조정
        val deviationPara = when {
            vibrationCount == 0 -> "긴장도 변화 없이 안정적으로 유지하셨어요.\n오늘도 좋은 흐름이었습니다."
            vibrationCount < 5 -> "긴장도가 몇 차례 오르내렸지만 곧 다시 안정을 찾으셨어요.\n좋은 회복력이에요."
            else -> "긴장도의 변화가 평소보다 자주 일어났어요.\n다음에는 좀 더 집중해보아요!"
        }

        return levelPara to deviationPara
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

    /** '랜덤 문구 추출형' 다채로운 AI 피드백 생성 알고리즘 */
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
            ).random()
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

    private fun showScreen(screen: Screen) {
        binding.layoutRecord.visibility = if (screen == Screen.RECORD) View.VISIBLE else View.GONE
        binding.layoutSaved.visibility = if (screen == Screen.SAVED) View.VISIBLE else View.GONE
        binding.layoutThread.visibility = if (screen == Screen.THREAD) View.VISIBLE else View.GONE
    }

    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("NAVIGATE_TO_HOME", true)
        startActivity(intent)
        finish()
    }

    private companion object {
        const val SAVED_FILL_TARGET = 65f
        const val SAVED_FILL_DURATION_MS = 1400L
        const val SAVED_SCREEN_DURATION_MS = 2600L

        // 원시 total 이론상 최댓값(4095*3)을 0~255 범위의 "긴장도" 배지 표시값으로 환산한다.
        const val RAW_PRESSURE_MAX = 12285f
        const val TENSION_BADGE_MAX = 255f
    }
}
