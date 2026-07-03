package com.newritage.app.ui.settings

/** 진동 설정 유형 */
enum class VibrationType {
    TIMER, TENSION
}

/**
 * 선택 가능한 진동 패턴.
 * @param timings 로컬 미리듣기(Vibrator.vibrate)용 on/off 패턴
 * @param effect 기기의 DRV2605L 라이브러리 효과 번호(1~123). BleManager.sendVibration()에 전달된다.
 */
data class VibrationPattern(
    val id: String,
    val name: String,
    val timings: LongArray,
    val effect: Int
)

object VibrationPatterns {
    val ALL = listOf(
        VibrationPattern("wave", "잔잔한 물결", longArrayOf(0, 120, 100, 120, 100, 200), effect = 51),
        VibrationPattern("tap", "가벼운 두드림", longArrayOf(0, 60, 80, 60), effect = 1),
        VibrationPattern("soft", "부드러운 파동", longArrayOf(0, 250), effect = 47),
        VibrationPattern("short", "짧은 알림", longArrayOf(0, 40), effect = 10),
        VibrationPattern("rhythm", "리듬 진동", longArrayOf(0, 80, 80, 80, 80, 160, 80, 80), effect = 16),
        VibrationPattern("rise", "점점 강해지는 진동", longArrayOf(0, 50, 60, 100, 60, 150), effect = 23),
        VibrationPattern("fall", "점점 약해지는 진동", longArrayOf(0, 150, 60, 100, 60, 50), effect = 24),
        VibrationPattern("heartbeat", "심장박동 진동", longArrayOf(0, 60, 60, 100, 200, 60, 60, 100), effect = 58)
    )
}
