package com.newritage.app.ble

/**
 * 명상 호흡 가이드 전용 마커 진동 효과 번호. 사용자가 선택하는 [VibrationPatterns]와 달리
 * 호흡 세션 엔진이 구간 경계에서 직접 기기에 전송하는 고정값이다.
 */
object BreathingCues {
    const val INHALE = 82
    const val EXHALE = 70
    const val HOLD = 7
    const val START = 10
    const val END = 16
}
