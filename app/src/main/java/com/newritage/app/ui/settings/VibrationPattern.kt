package com.newritage.app.ui.settings

/** 진동 설정 유형 */
enum class VibrationType {
    TIMER, TENSION
}

/** 선택 가능한 진동 패턴 (name = 진동 이름, timings = Vibrator.vibrate용 on/off 패턴) */
data class VibrationPattern(
    val id: String,
    val name: String,
    val timings: LongArray
)

object VibrationPatterns {
    val ALL = listOf(
        VibrationPattern("wave", "잔잔한 물결", longArrayOf(0, 120, 100, 120, 100, 200)),
        VibrationPattern("tap", "가벼운 두드림", longArrayOf(0, 60, 80, 60)),
        VibrationPattern("soft", "부드러운 파동", longArrayOf(0, 250)),
        VibrationPattern("short", "짧은 알림", longArrayOf(0, 40)),
        VibrationPattern("rhythm", "리듬 진동", longArrayOf(0, 80, 80, 80, 80, 160, 80, 80)),
        VibrationPattern("rise", "점점 강해지는 진동", longArrayOf(0, 50, 60, 100, 60, 150)),
        VibrationPattern("fall", "점점 약해지는 진동", longArrayOf(0, 150, 60, 100, 60, 50)),
        VibrationPattern("heartbeat", "심장박동 진동", longArrayOf(0, 60, 60, 100, 200, 60, 60, 100))
    )
}
