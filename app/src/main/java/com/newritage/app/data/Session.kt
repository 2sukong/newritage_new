package com.newritage.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** yyyy-MM-dd 형식 날짜 */
    val date: String,

    /** 하루 중 몇 번째 세션인지 (1부터) */
    val sessionIndex: Int = 1,

    /** 하루 첫 세션만 실(thread)을 받음 */
    val hasThread: Boolean = false,

    /** HH:mm 형식 */
    val startTime: String = "",
    val endTime: String = "",

    /** 명상 시간 (초) */
    val durationSeconds: Int,

    /** 평균/최고/최저/중앙값 압력 (kPa, overall 기준) */
    val avgPressure: Float,
    val maxPressure: Float,
    val minPressure: Float,
    val medianPressure: Float = 0f,

    /** 부위별 통계 — 분석 페이지용 */
    val thumbAvg: Float = 0f,
    val thumbMin: Float = 0f,
    val thumbMax: Float = 0f,
    val thumbMedian: Float = 0f,

    val imAvg: Float = 0f,
    val imMin: Float = 0f,
    val imMax: Float = 0f,
    val imMedian: Float = 0f,

    val palmAvg: Float = 0f,
    val palmMin: Float = 0f,
    val palmMax: Float = 0f,
    val palmMedian: Float = 0f,

    /** 진동(긴장 경고) 발생 횟수 */
    val vibrationCount: Int = 0,

    /** 오늘의 감정 메모 */
    val emotion: String = "",

    /** 실 색상 — hasThread가 true일 때만 값이 있음, hex */
    val threadColor: String = "",
    /** 실 색상의 한글 이름 */
    val threadColorName: String = "",

    /** 세션 완료 시 생성되는 AI 피드백 텍스트 */
    val aiFeedback: String = "",

    /** 생성 시각 (ms) */
    val createdAt: Long = System.currentTimeMillis()
)
