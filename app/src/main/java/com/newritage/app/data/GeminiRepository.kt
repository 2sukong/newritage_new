package com.newritage.app.data

import android.util.Log
import com.newritage.app.network.GeminiApi
import com.newritage.app.network.PromptBuilder
import com.newritage.app.ui.main.knot.model.KnotInfo
import com.newritage.app.ui.main.knot.model.KnotRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 매듭 추천 API 응답 — AI가 고른 매듭과 그 추천 이유. */
data class KnotRecommendation(val knot: KnotInfo, val reason: String)

/**
 * Gemini API(gemini-3.5-flash) 연동 저장소.
 *
 * AI(GeminiApi)는 DB에 직접 접근하지 않는다 — 이 Repository가 [sessionDao]로 필요한 데이터를 조회하고
 * [PromptBuilder]로 프롬프트를 구성한 뒤 [GeminiApi]를 호출한다. API 호출이 실패하면(키 누락, 네트워크 오류,
 * 응답 파싱 실패 등) null을 반환하므로, 호출자는 항상 API를 우선 시도하고 null일 때만 기존 로컬 로직으로
 * 폴백해야 한다.
 */
class GeminiRepository(private val sessionDao: SessionDao) {

    companion object {
        private const val TAG = "GeminiRepository"

        /** 압력이 이 값(kPa)을 넘으면 '이탈'로 간주한다 — MeasurementActivity의 이탈 카운트 기준과 동일. */
        private const val STABLE_THRESHOLD = 50f
    }

    /**
     * 명상 종료 직후 오늘의 세션 피드백 생성. 실패 시 null.
     * @param stableRatio 이번 세션의 안정 상태 비율(0~100).
     * @param readings 이번 세션의 원시 센서 데이터(시간순). 초반/중반/후반 압력 흐름 분석에 쓰인다.
     */
    suspend fun generateDailyFeedback(
        session: Session,
        stableRatio: Float,
        readings: List<SensorReading> = emptyList()
    ): String? = runCatching {
        val (system, user) = PromptBuilder.buildDailyFeedbackPrompt(session, stableRatio, readings)
        GeminiApi.chatCompletion(system, user)
    }.onFailure { Log.e(TAG, "일간 AI 피드백 생성 실패", it) }.getOrNull()

    /**
     * 하루 압력 변화 그래프 이미지([chartImageBase64], PNG를 base64로 인코딩한 값)를 첨부해
     * 오늘의 전반적인 명상 추세를 분석한 문구를 생성한다. 실패 시 null.
     */
    suspend fun generateDailyTrendFeedback(session: Session, chartImageBase64: String): String? = runCatching {
        val (system, user) = PromptBuilder.buildDailyTrendPrompt(session)
        GeminiApi.chatCompletion(system, user, imageBase64 = chartImageBase64)
    }.onFailure { Log.e(TAG, "일간 추세 이미지 분석 실패", it) }.getOrNull()

    /** [endDate](yyyy-MM-dd, 보통 오늘)로부터 최근 30일 세션을 바탕으로 월간 피드백 생성. 실패 시 null. */
    suspend fun generateMonthlyFeedback(endDate: String): String? {
        val startDate = shiftDate(endDate, -29)
        val sessions = sessionDao.getSessionsInRange(startDate, endDate)
        if (sessions.isEmpty()) return null

        return runCatching {
            val readings = sessionDao.getReadingsInRange(startDate, endDate)
            val stableRatio = stableRatioOf(readings)
            val (system, user) = PromptBuilder.buildMonthlyFeedbackPrompt(sessions, stableRatio)
            GeminiApi.chatCompletion(system, user)
        }.onFailure { Log.e(TAG, "월간 AI 피드백 생성 실패", it) }.getOrNull()
    }

    /** [endDate](yyyy-MM-dd, 보통 오늘)로부터 최근 30일 감정 기록을 바탕으로 매듭 추천. 실패 시 null. */
    suspend fun recommendKnot(endDate: String): KnotRecommendation? {
        val startDate = shiftDate(endDate, -29)
        val entries = sessionDao.getSessionsInRange(startDate, endDate)
            .filter { it.emotion.isNotBlank() }
            .map { it.date to it.emotion }
        if (entries.isEmpty()) return null

        return runCatching {
            val (system, user) = PromptBuilder.buildKnotRecommendationPrompt(entries)
            val raw = GeminiApi.chatCompletion(system, user)
            parseKnotRecommendation(raw)
        }.onFailure { Log.e(TAG, "매듭 추천 생성 실패", it) }.getOrNull()
    }

    /**
     * "추천 매듭:\n(이름)\n\n추천 이유:\n(문장)" 형식의 응답을 파싱한다.
     * 형식이 맞지 않거나 이름이 [KnotRepository] 후보 중 하나와 매칭되지 않으면 null(폴백 대상).
     */
    private fun parseKnotRecommendation(raw: String): KnotRecommendation? {
        val parts = raw.split(Regex("추천\\s*이유\\s*[:：]"), limit = 2)
        if (parts.size != 2) return null

        val nameText = parts[0].replace(Regex("추천\\s*매듭\\s*[:：]"), "").trim()
        val reason = parts[1].trim()
        if (nameText.isBlank() || reason.isBlank()) return null

        val normalized = nameText.removeSuffix("매듭").trim()
        val knot = KnotRepository.knots.firstOrNull {
            it.name == nameText || it.name.removeSuffix("매듭") == normalized
        } ?: return null

        return KnotRecommendation(knot, reason)
    }

    /** [readings] 중 압력([SensorReading.overall])이 [STABLE_THRESHOLD] 이하인 비율(%). 데이터가 없으면 100. */
    private fun stableRatioOf(readings: List<SensorReading>): Float {
        if (readings.isEmpty()) return 100f
        val stableCount = readings.count { it.overall <= STABLE_THRESHOLD }
        return stableCount.toFloat() / readings.size * 100f
    }

    private fun shiftDate(date: String, days: Int): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply {
            time = dateFormat.parse(date) ?: error("날짜 형식이 아닙니다: $date")
            add(Calendar.DAY_OF_MONTH, days)
        }
        return dateFormat.format(calendar.time)
    }
}
