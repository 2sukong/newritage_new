package com.newritage.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** yyyy-MM-dd 형식 날짜 */
    val date: String,

    /** 명상 시간 (초) */
    val durationSeconds: Int,

    /** 평균 압력 (kPa) */
    val avgPressure: Float,

    /** 최고 압력 (kPa) */
    val maxPressure: Float,

    /** 최저 압력 (kPa) */
    val minPressure: Float,

    /** 오늘의 감정 메모 */
    val emotion: String = "",

    /** 실 색상 (hex, 예: #FF5733) — 하드웨어 미연동 시 랜덤 할당 */
    val threadColor: String = "#8B9E7B",

    /** 생성 시각 (ms) */
    val createdAt: Long = System.currentTimeMillis()
)
