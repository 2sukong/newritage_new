package com.newritage.app.ui.main.analysis

import com.newritage.app.data.Session

/**
 * 이번 달 세션 기록을 바탕으로 AI 종합 코멘트를 문장으로 생성한다.
 * 숫자를 나열하지 않고 긴장도/명상 시간/명상 횟수/월초-월중-월후반 흐름을 자연스러운 문장으로 설명하며,
 * 마지막 문장은 항상 응원의 메시지로 마무리한다.
 */
object MonthlyCommentGenerator {

    fun generate(sessions: List<Session>): String {
        if (sessions.isEmpty()) {
            return "이번 달은 기록된 명상 데이터가 없습니다. 다음 달에는 편안한 마음으로 첫 걸음을 내딛어보세요."
        }

        val sorted = sessions.sortedBy { it.date }
        val early = sorted.filter { dayOfMonth(it.date) <= 10 }
        val mid = sorted.filter { dayOfMonth(it.date) in 11..20 }
        val late = sorted.filter { dayOfMonth(it.date) >= 21 }

        val overallPressure = sorted.map { it.avgPressure }.average()
        val earlyPressure = early.avgPressureOrNull()
        val midPressure = mid.avgPressureOrNull()
        val latePressure = late.avgPressureOrNull()

        val openingSentence = when {
            earlyPressure == null ->
                "이번 달은 중순 이후부터 명상 기록이 시작되었습니다."
            earlyPressure > overallPressure * 1.05 ->
                "월 초에는 긴장도가 다소 높은 편이었지만, 꾸준한 명상을 통해 점차 안정적인 흐름이 나타났습니다."
            earlyPressure < overallPressure * 0.95 ->
                "월 초부터 비교적 안정적인 긴장도로 시작해 편안한 흐름을 이어갔습니다."
            else ->
                "월 초부터 일정한 긴장도를 유지하며 명상을 시작했습니다."
        }

        val midSentence = when {
            midPressure != null && latePressure != null && latePressure < midPressure ->
                "중반을 지나며 긴장의 변화 폭이 점차 줄어들었고, 후반으로 갈수록 더욱 편안한 흐름이 이어졌습니다."
            midPressure != null && earlyPressure != null && midPressure < earlyPressure ->
                "중반에 접어들며 긴장도가 한층 누그러지는 모습을 보였습니다."
            else ->
                "한 달 내내 큰 굴곡 없이 꾸준한 흐름 속에서 명상이 이어졌습니다."
        }

        val earlyTime = early.sumOf { it.durationSeconds }
        val lateTime = late.sumOf { it.durationSeconds }
        val timeTrendSentence = when {
            early.isEmpty() || late.isEmpty() ->
                "명상 시간과 횟수는 한 달 동안 꾸준히 이어졌습니다."
            lateTime > earlyTime && late.size >= early.size ->
                "명상 시간과 횟수 모두 후반으로 갈수록 점차 늘어나는 긍정적인 변화를 보였습니다."
            lateTime > earlyTime ->
                "명상 시간이 점차 늘어나며 더 깊이 있는 명상 습관이 자리잡는 모습이었습니다."
            late.size >= early.size ->
                "명상 횟수가 꾸준히 늘어나며 명상이 일상의 습관으로 자리잡아가고 있습니다."
            else ->
                "명상 시간과 횟수는 한 달 내내 안정적으로 유지되었습니다."
        }

        val closingSentence =
            "이번 달은 전반적으로 안정적인 명상 습관이 형성되고 있는 긍정적인 흐름을 보여주었습니다. " +
                "다음 달에도 지금처럼 꾸준히 이어간다면 더욱 편안한 명상 경험을 만들 수 있을 것입니다."

        return listOf(openingSentence, midSentence, timeTrendSentence, closingSentence).joinToString("\n")
    }

    private fun dayOfMonth(date: String): Int = date.substringAfterLast("-").toIntOrNull() ?: 15

    private fun List<Session>.avgPressureOrNull(): Double? =
        if (isEmpty()) null else map { it.avgPressure }.average()
}
