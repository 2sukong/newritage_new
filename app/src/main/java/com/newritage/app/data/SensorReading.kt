package com.newritage.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_readings")
data class SensorReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val thumb: Float,
    val indexMiddle: Float,
    val palm: Float,
    val overall: Float
)