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

        // 부위별 기준 압력 (BLE SENSOR 특성의 f0/f1/f2/total 각각의 평균, raw ADC 0~12285)
        private const val KEY_BASELINE_THUMB = "baseline_thumb"
        private const val KEY_BASELINE_IM = "baseline_im"
        private const val KEY_BASELINE_PALM = "baseline_palm"
        private const val KEY_BASELINE_OVERALL = "baseline_overall"

        private const val KEY_AUTO_LOGIN = "auto_login"
        private const val KEY_LAST_SESSION_ID = "last_session_id"
        private const val KEY_TIMER_VIBRATION_ENABLED = "timer_vibration_enabled"
        private const val KEY_TENSION_VIBRATION_ENABLED = "tension_vibration_enabled"
        private const val KEY_TIMER_VIBRATION_PATTERN_ID = "timer_vibration_pattern_id"
        private const val KEY_TENSION_VIBRATION_PATTERN_ID = "tension_vibration_pattern_id"

        private const val KEY_TENSION_THRESHOLD_RATIO = "tension_threshold_ratio"
        private const val KEY_TIMER_DURATION_MINUTES = "timer_duration_minutes"

        private const val KEY_MONTHLY_AI_COMMENT = "monthly_ai_comment"
        private const val KEY_MONTHLY_AI_COMMENT_SESSION_ID = "monthly_ai_comment_session_id"

        private const val KEY_DEV_DATE_OFFSET_DAYS = "dev_date_offset_days"

        // 디버그 토글
        private const val KEY_DEBUG_DATE_BAR_VISIBLE = "debug_date_bar_visible"
        private const val KEY_DEBUG_SHOW_ALL_COLLECTION = "debug_show_all_collection"
        private const val KEY_REQUIRE_BLE_TO_START = "require_ble_to_start"
    }

    /**
     * 기기(BLE)가 연결되어 있지 않으면 측정 시작을 막을지 여부. 기본값 false(연결 여부와 무관하게
     * 바로 측정 시작 가능 — 기존 FeatureFlags 기본 동작 유지). 메인 측정과 초기 기준 측정
     * 시작 지점에서 이 값을 확인한다.
     */
    var requireBleConnectionToStart: Boolean
        get() = prefs.getBoolean(KEY_REQUIRE_BLE_TO_START, false)
        set(value) = prefs.edit().putBoolean(KEY_REQUIRE_BLE_TO_START, value).apply()

    /**
     * 디버그: 메인 화면 하단의 시연용 날짜 전환 바를 보일지 여부. 기본값 true(기존 동작 유지).
     * [com.newritage.app.ui.main.MainActivity]가 이 값으로 날짜바 visibility를 토글한다.
     */
    var debugDateBarVisible: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_DATE_BAR_VISIBLE, true)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_DATE_BAR_VISIBLE, value).apply()

    /**
     * 디버그: 실/매듭 보관함에 실제로 모은 것 대신 전체 실 색상(12종)과 전체 매듭(10종) 목록을
     * 보여줄지 여부. 기본값 false(직접 모은 것만 표시). DB는 건드리지 않는 화면 표시 전용 오버라이드라
     * off로 되돌리면 모았던 실/매듭이 그대로 다시 보인다.
     */
    var debugShowAllCollection: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_SHOW_ALL_COLLECTION, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_SHOW_ALL_COLLECTION, value).apply()

    /**
     * 시연용 개발자 기능: 앱이 "오늘"로 볼 가상 날짜를 실제 오늘로부터 며칠 밀지 저장한다.
     * 0이면 실제 오늘. [com.newritage.app.util.DevClock]가 이 값을 읽어 가상 오늘을 계산한다.
     */
    var devDateOffsetDays: Int
        get() = prefs.getInt(KEY_DEV_DATE_OFFSET_DAYS, 0)
        set(value) = prefs.edit().putInt(KEY_DEV_DATE_OFFSET_DAYS, value).apply()

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

    /** 기준 압력 값 - 엄지 */
    var baselineThumb: Float
        get() = prefs.getFloat(KEY_BASELINE_THUMB, 30f)
        set(value) = prefs.edit().putFloat(KEY_BASELINE_THUMB, value).apply()

    /** 기준 압력 값 - 검지·중지 */
    var baselineIM: Float
        get() = prefs.getFloat(KEY_BASELINE_IM, 30f)
        set(value) = prefs.edit().putFloat(KEY_BASELINE_IM, value).apply()

    /** 기준 압력 값 - 손바닥 */
    var baselinePalm: Float
        get() = prefs.getFloat(KEY_BASELINE_PALM, 30f)
        set(value) = prefs.edit().putFloat(KEY_BASELINE_PALM, value).apply()

    /** 기준 압력 값 - 전체 (f0+f1+f2) */
    var baselineOverall: Float
        get() = prefs.getFloat(KEY_BASELINE_OVERALL, 30f)
        set(value) = prefs.edit().putFloat(KEY_BASELINE_OVERALL, value).apply()

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

    /** 긴장 진동 발동 임계 비율 (baseline * ratio 이상이면 긴장으로 분류) */
    var tensionThresholdRatio: Float
        get() = prefs.getFloat(KEY_TENSION_THRESHOLD_RATIO, 1.2f)
        set(value) = prefs.edit().putFloat(KEY_TENSION_THRESHOLD_RATIO, value).apply()

    /** 타이머 진동 카운트다운 시간(분) */
    var timerDurationMinutes: Int
        get() = prefs.getInt(KEY_TIMER_DURATION_MINUTES, 10)
        set(value) = prefs.edit().putInt(KEY_TIMER_DURATION_MINUTES, value).apply()

    /** 마지막으로 생성된 월간 AI 종합 코멘트. [monthlyAiCommentSessionId]와 함께 캐시로 사용된다. */
    var monthlyAiComment: String
        get() = prefs.getString(KEY_MONTHLY_AI_COMMENT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MONTHLY_AI_COMMENT, value).apply()

    /**
     * [monthlyAiComment]를 생성할 당시의 최신 세션 id.
     * 이후 최신 세션 id가 그대로면(=새로 명상한 기록이 없으면) 캐시된 코멘트를 그대로 재사용한다.
     */
    var monthlyAiCommentSessionId: Long
        get() = prefs.getLong(KEY_MONTHLY_AI_COMMENT_SESSION_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_MONTHLY_AI_COMMENT_SESSION_ID, value).apply()

    /** 로그아웃 */
    fun logout() {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .putString(KEY_USERNAME, "")
            .apply()
    }
}
