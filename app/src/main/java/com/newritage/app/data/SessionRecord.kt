package com.newritage.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_records")
data class SessionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val baselineThumb: Float,
    val baselineIM: Float,
    val baselinePalm: Float,
    val baselineOverall: Float,
    val vibrationCount: Int = 0
)