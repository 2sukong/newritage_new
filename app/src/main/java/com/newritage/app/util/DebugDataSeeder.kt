package com.newritage.app.util

import android.content.Context
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.Session
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 실/매듭 보관함이 아이템으로 찬 모습을 테스트하기 위한 더미 데이터 시더.
 * ENABLED를 true로 바꾸고 앱을 실행하면
 *   1) 실 보관함용: 오늘부터 DAYS일 전까지 하루 한 개씩 실을 얻은 세션
 *   2) 매듭 보관함용: 최근 여러 달에 걸쳐 7종 매듭이 "모두" 추천되도록, 각 달마다 특정 매듭으로
 *      분류되는 감정 일기 + 실 색상을 한 개씩
 * 를 채워 넣는다(이미 세션이 있는 날짜는 건너뛰므로 여러 번 실행해도 안전).
 * 다 확인했으면 ENABLED를 다시 false로 돌려두면 된다.
 */
object DebugDataSeeder {
    const val ENABLED = true
    const val DAYS = 20

    /**
     * 매듭 보관함에 7종 매듭이 전부 보이도록, 달마다 서로 다른 매듭으로 추천되게 하는 감정 문장.
     * 각 문장은 RecommendationEngine(KeywordDictionary 기반)이 아래 주석의 매듭으로 분류하도록
     * 해당 매듭의 대표 키워드만 담았다. 실제 추천 결과는 앱 실행 후 매듭 보관함에서 눈으로 검증한다.
     */
    private val KNOT_SAMPLE_EMOTIONS = listOf(
        "새로운 마음으로 다시 시작했고 도전할 용기가 생겼다",          // dorrae      -> 도래매듭
        "매일 꾸준히 반복하며 좋은 습관을 이어갔다",                  // twin_flower -> 생쪽매듭
        "끝까지 인내하고 이겨냈고 다시 희망이 생겼다",                // plum        -> 매화매듭
        "마음이 평온하고 안정되어 여유롭고 차분한 하루였다",          // chrysanthemum -> 국화매듭
        "가족과 함께한 시간이 행복하고 감사했다",                    // butterfly   -> 가락지매듭
        "생활의 균형과 조화를 지키며 건강한 하루를 보냈다",          // samjeongja  -> 삼정자매듭
        "친구와 오해를 풀고 화해하며 서로 소통하고 공감했다"          // glasses     -> 안경매듭
    )

    suspend fun seedIfEnabled(context: Context) {
        if (!ENABLED) return

        val dao = AppDatabase.getInstance(context).sessionDao()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 1) 매듭 보관함용: 최근 KNOT_SAMPLE_EMOTIONS.size 개월에 각각 한 개씩
        //    (감정 일기 + 실 색상 + hasThread) 세션을 심어 7종 매듭이 모두 보이게 한다.
        //    아래 일별 실 시딩보다 먼저 심어야 이번 달 15일이 실 세션에 선점되지 않는다.
        val monthCal = Calendar.getInstance()
        for ((index, emotion) in KNOT_SAMPLE_EMOTIONS.withIndex()) {
            monthCal.time = Calendar.getInstance().time
            monthCal.add(Calendar.MONTH, -index)
            monthCal.set(Calendar.DAY_OF_MONTH, 15)
            val dateStr = sdf.format(monthCal.time)
            if (dao.countSessionsByDate(dateStr) == 0) {
                val color = ThreadColors.ALL[index % ThreadColors.ALL.size]
                dao.insert(
                    Session(
                        date = dateStr,
                        hasThread = true,
                        durationSeconds = 300,
                        avgPressure = 30f,
                        maxPressure = 40f,
                        minPressure = 20f,
                        emotion = emotion,
                        threadColor = color.hex,
                        threadColorName = color.nameKr
                    )
                )
            }
        }

        // 2) 실 보관함용: 최근 DAYS일 하루 한 개씩 실 세션
        //    (이미 매듭 샘플이 심긴 15일 등은 countSessionsByDate 가드로 건너뛴다)
        val calendar = Calendar.getInstance()
        for (dayOffset in 0 until DAYS) {
            val dateStr = sdf.format(calendar.time)
            if (dao.countSessionsByDate(dateStr) == 0) {
                val color = ThreadColors.ALL[dayOffset % ThreadColors.ALL.size]
                dao.insert(
                    Session(
                        date = dateStr,
                        hasThread = true,
                        durationSeconds = 300,
                        avgPressure = 30f,
                        maxPressure = 40f,
                        minPressure = 20f,
                        threadColor = color.hex,
                        threadColorName = color.nameKr
                    )
                )
            }
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
    }
}
