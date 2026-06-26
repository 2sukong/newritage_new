package com.newritage.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)

    /** 특정 날짜의 세션 목록 */
    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY createdAt DESC")
    suspend fun getSessionsByDate(date: String): List<Session>

    /** 특정 날짜의 가장 최근 세션 1개 */
    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSessionByDate(date: String): Session?

    /** 특정 기간 세션 목록 */
    @Query("SELECT * FROM sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getSessionsInRange(startDate: String, endDate: String): List<Session>

    /** 특정 연월의 세션 목록 (yyyy-MM) */
    @Query("SELECT * FROM sessions WHERE date LIKE :yearMonth || '%' ORDER BY date ASC")
    suspend fun getSessionsByMonth(yearMonth: String): List<Session>

    /** 전체 세션 (Flow) */
    @Query("SELECT * FROM sessions ORDER BY date DESC, createdAt DESC")
    fun getAllSessionsFlow(): Flow<List<Session>>

    /** ID로 조회 */
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): Session?

    /** 감정 업데이트 */
    @Query("UPDATE sessions SET emotion = :emotion WHERE id = :id")
    suspend fun updateEmotion(id: Long, emotion: String)
}
