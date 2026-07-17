package com.newritage.app.data

class SessionRepository(
    private val sessionDao: SessionDao,
    private val prefs: UserPreferences
) {

    /**
     * 하루 첫 세션(실 배정 대상) 저장.
     * newSession.avgPressure(그 세션만의 평균)와 baseline을 비교해 실 색상을 확정.
     * 이후 같은 날 추가되는 세션들은 이 함수를 타지 않으므로 실 색상엔 영향 없음.
     */
    suspend fun createFirstSessionOfDay(newSession: Session): Session {
        val baseline = prefs.baselineOverall

        val changeRate = if (baseline != 0f) {
            ((newSession.avgPressure - baseline) / baseline) * 100f
        } else 0f

        val state = ThreadColorManager.classifyState(changeRate)
        val thread = ThreadColorManager.getRandomThreadForState(state)

        val sessionWithThread = newSession.copy(
            hasThread = true,
            threadColor = "",              // 더 이상 hex 안 쓰므로 빈 값
            threadColorName = thread.colorName
        )

        val id = sessionDao.insert(sessionWithThread)
        return sessionWithThread.copy(id = id)
    }
}