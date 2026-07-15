package com.newritage.app.util

import android.content.Context
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.Session
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 실/매듭 보관함이 아이템으로 찬 모습을 테스트하기 위한 더미 데이터 시더.
 * ENABLED를 true로 바꾸고 앱을 실행하면 오늘부터 DAYS일 전까지 하루 한 개씩
 * 실을 얻은 세션을 채워 넣는다(이미 세션이 있는 날짜는 건너뛰므로 여러 번 실행해도 안전).
 * 다 확인했으면 ENABLED를 다시 false로 돌려두면 된다.
 */
object DebugDataSeeder {
    const val ENABLED = true
    const val DAYS = 20

    suspend fun seedIfEnabled(context: Context) {
        if (!ENABLED) return

        val dao = AppDatabase.getInstance(context).sessionDao()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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
