package com.newritage.app.stats

import com.newritage.app.data.SensorReading

data class PressureStats(
    val max: Float,
    val min: Float,
    val avg: Float,
    val median: Float
)

class StatsCalculator {

    fun calculate(values: List<Float>): PressureStats {
        val sorted = values.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
        } else {
            sorted[sorted.size / 2]
        }
        return PressureStats(
            max = values.max(),
            min = values.min(),
            avg = values.average().toFloat(),
            median = median
        )
    }

    fun calculateDailyReport(readings: List<SensorReading>): Map<String, PressureStats> {
        return mapOf(
            "overall" to calculate(readings.map { it.overall }),
            "thumb" to calculate(readings.map { it.thumb }),
            "indexMiddle" to calculate(readings.map { it.indexMiddle }),
            "palm" to calculate(readings.map { it.palm })
        )
    }
}