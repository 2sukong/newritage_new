package com.newritage.app.util

import android.content.Context
import com.newritage.app.data.UserPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 시연용 개발자 기능: 앱 전체가 공유하는 "가상 오늘" 날짜 제공자.
 *
 * 실제 시스템 날짜에 [UserPreferences.devDateOffsetDays]만큼 더한 날짜를 "오늘"로 돌려준다.
 * 오프셋이 0이면 실제 오늘과 같다. 메인 화면의 개발자 날짜바에서 오프셋을 ±1일씩 조정하며,
 * 실 획득 판정(SessionCompleteActivity)과 보관함 NEW 배지가 이 값을 기준으로 동작한다.
 *
 * 실제 배포 시에는 날짜바를 숨기고 오프셋을 0으로 두면 실제 날짜대로 동작한다.
 */
object DevClock {

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun today(prefs: UserPreferences): LocalDate =
        LocalDate.now().plusDays(prefs.devDateOffsetDays.toLong())

    fun today(context: Context): LocalDate = today(UserPreferences(context))

    /** "yyyy-MM-dd" 형식의 가상 오늘. Session.date와 같은 포맷. */
    fun todayString(prefs: UserPreferences): String = today(prefs).format(DATE_FORMAT)

    fun todayString(context: Context): String = today(context).format(DATE_FORMAT)

    /** "yyyy-MM" 형식의 가상 이번 달. */
    fun yearMonthString(prefs: UserPreferences): String = today(prefs).format(MONTH_FORMAT)
}
