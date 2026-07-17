package com.newritage.app.ui.main.analysis.model

import com.newritage.app.data.Session

/**
 * 저번주/저번달 대비 이번 기간의 변화량.
 * 압력 항목은 퍼센트, 명상 시간/횟수는 실제 증감값으로 표현한다.
 * 이전 기간에 데이터가 없으면 각 필드는 null(비교 불가)이 된다.
 */
data class ComparisonSummary(
    val avgPressureChangePercent: Float?,
    val maxPressureChangePercent: Float?,
    val meditationTimeChangeSeconds: Int?,
    val meditationCountChange: Int?
) {
    companion object {
        fun from(current: List<Session>, previous: List<Session>): ComparisonSummary {
            if (current.isEmpty() || previous.isEmpty()) {
                return ComparisonSummary(null, null, null, null)
            }

            val curAvg = current.map { it.avgPressure }.average().toFloat()
            val prevAvg = previous.map { it.avgPressure }.average().toFloat()
            val curMax = current.maxOf { it.maxPressure }
            val prevMax = previous.maxOf { it.maxPressure }
            val curTime = current.sumOf { it.durationSeconds }
            val prevTime = previous.sumOf { it.durationSeconds }

            return ComparisonSummary(
                avgPressureChangePercent = if (prevAvg != 0f) (curAvg - prevAvg) / prevAvg * 100f else null,
                maxPressureChangePercent = if (prevMax != 0f) (curMax - prevMax) / prevMax * 100f else null,
                meditationTimeChangeSeconds = curTime - prevTime,
                meditationCountChange = current.size - previous.size
            )
        }
    }
}
