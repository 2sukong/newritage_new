package com.newritage.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: Session): Long

    @Insert
    suspend fun insertReadings(readings: List<SensorReading>)

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)

    /** 특정 날짜의 세션 목록 */
    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY createdAt ASC")
    suspend fun getSessionsByDate(date: String): List<Session>

    /** 특정 날짜의 가장 최근 세션 1개 */
    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSessionByDate(date: String): Session?

    /** 특정 날짜의 세션 개수 */
    @Query("SELECT COUNT(*) FROM sessions WHERE date = :date")
    suspend fun countSessionsByDate(date: String): Int

    /** 특정 날짜의 실(thread) 세션 (하루 첫 세션) */
    @Query("SELECT * FROM sessions WHERE date = :date AND hasThread = 1 LIMIT 1")
    suspend fun getThreadSessionByDate(date: String): Session?

    /** 특정 기간 세션 목록 */
    @Query("SELECT * FROM sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, createdAt ASC")
    suspend fun getSessionsInRange(startDate: String, endDate: String): List<Session>

    /** 특정 연월의 세션 목록 (yyyy-MM) */
    @Query("SELECT * FROM sessions WHERE date LIKE :yearMonth || '%' ORDER BY date ASC, createdAt ASC")
    suspend fun getSessionsByMonth(yearMonth: String): List<Session>

    /** 특정 연도의 세션 목록 (yyyy) */
    @Query("SELECT * FROM sessions WHERE date LIKE :year || '%' ORDER BY date ASC, createdAt ASC")
    suspend fun getSessionsByYear(year: String): List<Session>

    /** 전체 세션 (Flow) */
    @Query("SELECT * FROM sessions ORDER BY date DESC, createdAt DESC")
    fun getAllSessionsFlow(): Flow<List<Session>>

    /** ID로 조회 */
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): Session?

    /** 감정 업데이트 */
    @Query("UPDATE sessions SET emotion = :emotion WHERE id = :id")
    suspend fun updateEmotion(id: Long, emotion: String)

    /** 진동 횟수 업데이트 */
    @Query("UPDATE sessions SET vibrationCount = :count WHERE id = :sessionId")
    suspend fun updateVibrationCount(sessionId: Long, count: Int)

    /** 특정 연월에 세션이 있는 날짜 목록 (yyyy-MM) */
    @Query("SELECT DISTINCT date FROM sessions WHERE date LIKE :yearMonth || '%' ORDER BY date ASC")
    suspend fun getDatesWithSessionsByMonth(yearMonth: String): List<String>

    /** 전체 활동 일수 (연속일수 계산용) */
    @Query("SELECT COUNT(DISTINCT date) FROM sessions")
    suspend fun getTotalActiveDays(): Int

    /** 전체 세션 개수 (생애 최초 세션 여부 판단용) */
    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun countAllSessions(): Int

    /** 세션 하나에 속한 원시 센서 데이터 */
    @Query("SELECT * FROM sensor_readings WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getReadingsForSession(sessionId: Long): List<SensorReading>

    /** 특정 날짜에 속한 모든 원시 데이터 (일간 분석용) */
    @Query(
        """
        SELECT sensor_readings.* FROM sensor_readings
        INNER JOIN sessions ON sensor_readings.sessionId = sessions.id
        WHERE sessions.date = :date
        ORDER BY sensor_readings.timestamp ASC
        """
    )
    suspend fun getReadingsByDate(date: String): List<SensorReading>

    /** 날짜 범위의 원시 데이터 (주간 분석용) */
    @Query(
        """
        SELECT sensor_readings.* FROM sensor_readings
        INNER JOIN sessions ON sensor_readings.sessionId = sessions.id
        WHERE sessions.date BETWEEN :startDate AND :endDate
        ORDER BY sensor_readings.timestamp ASC
        """
    )
    suspend fun getReadingsInRange(startDate: String, endDate: String): List<SensorReading>

    /** 월 패턴의 원시 데이터 (월간 분석용) */
    @Query(
        """
        SELECT sensor_readings.* FROM sensor_readings
        INNER JOIN sessions ON sensor_readings.sessionId = sessions.id
        WHERE sessions.date LIKE :yearMonthPattern
        ORDER BY sensor_readings.timestamp ASC
        """
    )
    suspend fun getReadingsByMonth(yearMonthPattern: String): List<SensorReading>
}
