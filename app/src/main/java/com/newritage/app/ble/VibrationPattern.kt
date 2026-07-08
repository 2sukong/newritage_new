package com.newritage.app.ble

/** 진동 설정 유형 */
enum class VibrationType {
    TIMER, TENSION
}

/**
 * 선택 가능한 진동 패턴. 기기의 DRV2605L 라이브러리 효과 번호(1~123)를
 * [count]회, [intervalMs](ms) 간격으로 BleManager.sendVibration()에 그대로 전달한다.
 * 폰 자체 진동(Vibrator)은 사용하지 않는다 — 판단·설정은 앱, 실행은 기기라는 원칙에 따름.
 */
data class VibrationPattern(
    val id: String,
    val name: String,
    val type: VibrationType,
    val effect: Int,
    val count: Int = 1,
    val intervalMs: Int = 0
)

// TODO: LRA 실기 테스트 후 효과#/count/interval 최종값 조정
object VibrationPatterns {
    val ALL = listOf(
        VibrationPattern("soft_tap", "부드러운 툭", VibrationType.TENSION, effect = 7),
        VibrationPattern("hum", "잔잔한 허밍", VibrationType.TENSION, effect = 119),
        VibrationPattern("double_tap", "이중 탭", VibrationType.TIMER, effect = 10),
        VibrationPattern("triple_tap", "삼중 탭", VibrationType.TIMER, effect = 12),
        VibrationPattern("long_alert", "긴 알림", VibrationType.TIMER, effect = 16),
        VibrationPattern("repeat_ring", "울림 반복", VibrationType.TIMER, effect = 14, count = 3, intervalMs = 600)
    )

    /** 긴장도 진동 기본값: 부드러운 툭(effect 7) */
    val TENSION_DEFAULT_ID = "soft_tap"

    /** 타이머 진동 기본값: 이중 탭(effect 10) */
    val TIMER_DEFAULT_ID = "double_tap"
}
