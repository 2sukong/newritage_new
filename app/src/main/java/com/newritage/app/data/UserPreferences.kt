package com.newritage.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 사용자 설정 및 앱 상태를 SharedPreferences로 관리
 */
class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("newritage_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_BASELINE_DONE = "baseline_done"
        private const val KEY_BASELINE_PRESSURE = "baseline_pressure"
        private const val KEY_AUTO_LOGIN = "auto_login"
        private const val KEY_LAST_SESSION_ID = "last_session_id"
        private const val KEY_TIMER_VIBRATION_ENABLED = "timer_vibration_enabled"
        private const val KEY_TENSION_VIBRATION_ENABLED = "tension_vibration_enabled"
        private const val KEY_TIMER_VIBRATION_PATTERN_ID = "timer_vibration_pattern_id"
        private const val KEY_TENSION_VIBRATION_PATTERN_ID = "tension_vibration_pattern_id"
    }

    /** 온보딩 완료 여부 */
    var isOnboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    /** 로그인 상태 */
    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()

    /** 사용자 이름 */
    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    /** 기준 압력 측정 완료 여부 */
    var isBaselineDone: Boolean
        get() = prefs.getBoolean(KEY_BASELINE_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_BASELINE_DONE, value).apply()

    /** 기준 압력 값 (kPa) */
    var baselinePressure: Float
        get() = prefs.getFloat(KEY_BASELINE_PRESSURE, 30f)
        set(value) = prefs.edit().putFloat(KEY_BASELINE_PRESSURE, value).apply()

    /** 자동로그인 여부 */
    var autoLogin: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOGIN, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LOGIN, value).apply()

    /** 마지막 세션 ID */
    var lastSessionId: Long
        get() = prefs.getLong(KEY_LAST_SESSION_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_SESSION_ID, value).apply()

    /** 타이머 진동 사용 여부 */
    var isTimerVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_TIMER_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TIMER_VIBRATION_ENABLED, value).apply()

    /** 긴장도 진동 사용 여부 */
    var isTensionVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_TENSION_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TENSION_VIBRATION_ENABLED, value).apply()

    /** 타이머 진동 패턴 ID */
    var timerVibrationPatternId: String?
        get() = prefs.getString(KEY_TIMER_VIBRATION_PATTERN_ID, null)
        set(value) = prefs.edit().putString(KEY_TIMER_VIBRATION_PATTERN_ID, value).apply()

    /** 긴장도 진동 패턴 ID */
    var tensionVibrationPatternId: String?
        get() = prefs.getString(KEY_TENSION_VIBRATION_PATTERN_ID, null)
        set(value) = prefs.edit().putString(KEY_TENSION_VIBRATION_PATTERN_ID, value).apply()

    /** 로그아웃 */
    fun logout() {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .putString(KEY_USERNAME, "")
            .apply()
    }
}
