package com.newritage.app.ui.main.analysis

import com.newritage.app.data.Session

/**
 * 이번 달 세션 기록을 바탕으로 AI 종합 코멘트를 문장으로 생성한다.
 * 숫자를 나열하지 않고 긴장도/명상 시간/명상 횟수/월초-월중-월후반 흐름을 자연스러운 문장으로 설명하며,
 * 마지막 문장은 총 명상 횟수에 맞는 응원의 메시지로 마무리한다.
 *
 * 기록이 적을 때 실제로는 판단할 수 없는 추세("꾸준히 늘었다" 등)를 단정하지 않도록,
 * 각 문장은 근거로 삼는 구간(월 전체 / 월초·중·후반)에 [MIN_PERIOD_SESSIONS]·[MIN_LIMITED_SESSIONS]·
 * [MIN_FULL_SESSIONS] 이상의 기록이 있을 때만 비교를 수행하고, 그렇지 않으면 "판단하기 어렵다"는
 * 문장으로 대체한다.
 */
object MonthlyCommentGenerator {

    /** 이 미만이면(1~2회) 추세 비교 자체를 하지 않는다. */
    private const val MIN_LIMITED_SESSIONS = 3

    /** 이 이상이어야(6회~) 월초/중/후반 3구간 비교까지 수행한다. 미만이면 제한적 분석만 한다. */
    private const val MIN_FULL_SESSIONS = 6

    /** 월초/중/후반 등 특정 구간끼리 비교하려면 그 구간에 최소 이만큼의 기록이 있어야 한다. */
    private const val MIN_PERIOD_SESSIONS = 2

    fun generate(sessions: List<Session>): String {
        if (sessions.isEmpty()) {
            return "이번 달은 기록된 명상 데이터가 없습니다. 다음 달에는 편안한 마음으로 첫 걸음을 내딛어보세요."
        }

        val sorted = sessions.sortedBy { it.date }
        val totalSessions = sorted.size

        val early = sorted.filter { dayOfMonth(it.date) <= 10 }
        val mid = sorted.filter { dayOfMonth(it.date) in 11..20 }
        val late = sorted.filter { dayOfMonth(it.date) >= 21 }

        val overallPressure = sorted.map { it.avgPressure }.average()
        val hasFullTrend = totalSessions >= MIN_FULL_SESSIONS

        val opening = openingSentence(early, overallPressure)
        val midFlow = midSentence(early, mid, late, hasFullTrend)
        val timeTrend = timeTrendSentence(early, late, totalSessions)
        val closing = closingSentence(totalSessions)

        return listOf(opening, midFlow, timeTrend, closing).joinToString("\n")
    }

    /** 특정 구간(월초/중/후반)끼리 비교하기에 기록이 충분한지 여부. */
    private fun sufficientForPeriodCompare(period: List<Session>) = period.size >= MIN_PERIOD_SESSIONS

    private fun openingSentence(early: List<Session>, overallPressure: Double): String = when {
        early.isEmpty() -> listOf(
            "이번 달은 중순 이후부터 명상 기록이 시작되었습니다.",
            "월 초에는 기록이 없어, 이번 달은 중반부터 명상을 시작하신 것으로 보입니다.",
            "이번 달의 명상은 초반이 아닌 중순 무렵부터 이어졌습니다.",
            "월 초 기록은 없지만, 그 이후로 꾸준히 명상을 이어가신 흔적이 보입니다.",
            "이번 달은 초반을 지나 명상을 시작하신 한 달이었습니다.",
            "월 초보다는 중반 이후에 명상 습관을 들이기 시작하신 것 같습니다."
        ).random()

        !sufficientForPeriodCompare(early) -> listOf(
            "월 초 기록이 많지 않아 그 시기의 흐름을 판단하기는 어렵습니다.",
            "월 초에는 기록이 한두 차례뿐이라 이 시기의 경향을 말씀드리기는 이릅니다.",
            "월 초의 흐름을 판단하기에는 아직 기록이 부족합니다.",
            "이번 달 초반의 상태를 가늠하기에는 기록이 충분치 않습니다.",
            "월 초 기록이 드문 편이라, 그 시기의 변화를 짚어내기는 어렵습니다.",
            "월 초의 긴장도 흐름은 기록이 적어 뚜렷하게 설명드리기 어렵습니다."
        ).random()

        else -> {
            val earlyPressure = early.avgPressureOrNull()!!
            when {
                earlyPressure > overallPressure * 1.05 -> listOf(
                    "월 초에는 긴장도가 다소 높은 편이었지만, 이후 점차 안정적인 흐름으로 이어졌습니다.",
                    "이번 달은 초반의 긴장도가 비교적 높게 나타났지만, 시간이 지나며 차츰 누그러졌습니다.",
                    "월 초에는 다소 긴장된 상태로 시작했으나, 이후 흐름은 한결 편안해졌습니다.",
                    "이번 달 초반에는 긴장도가 평소보다 높게 측정되었습니다.",
                    "월 초의 긴장도는 다른 시기에 비해 다소 높은 편이었습니다.",
                    "이번 달은 처음에 긴장도가 높게 나타났다가 이후 완만해지는 흐름을 보였습니다."
                ).random()

                earlyPressure < overallPressure * 0.95 -> listOf(
                    "월 초부터 비교적 안정적인 긴장도로 시작해 편안한 흐름을 이어갔습니다.",
                    "이번 달은 초반부터 긴장도가 낮게 유지되며 차분하게 시작되었습니다.",
                    "월 초의 긴장도는 다른 시기보다 안정적인 편이었습니다.",
                    "이번 달은 처음부터 편안한 흐름으로 명상을 시작하셨습니다.",
                    "월 초부터 몸과 마음이 비교적 이완된 상태였던 것으로 보입니다.",
                    "이번 달 초반에는 긴장도가 낮은 편으로, 안정적으로 출발했습니다."
                ).random()

                else -> listOf(
                    "월 초부터 일정한 긴장도를 유지하며 명상을 시작했습니다.",
                    "이번 달은 초반부터 큰 변화 없이 비슷한 긴장도로 시작되었습니다.",
                    "월 초의 긴장도는 이번 달 전체 흐름과 크게 다르지 않았습니다.",
                    "이번 달은 처음부터 안정된 리듬으로 명상을 이어가셨습니다.",
                    "월 초의 상태는 이후 흐름과 비교해도 큰 차이가 없었습니다.",
                    "이번 달은 초반부터 꾸준한 긴장도 수준을 유지했습니다."
                ).random()
            }
        }
    }

    private fun midSentence(
        early: List<Session>,
        mid: List<Session>,
        late: List<Session>,
        hasFullTrend: Boolean
    ): String {
        val midPressure = mid.avgPressureOrNull()
        val earlyPressure = early.avgPressureOrNull()
        val latePressure = late.avgPressureOrNull()

        val canCompareMidToLate = hasFullTrend && sufficientForPeriodCompare(mid) &&
            sufficientForPeriodCompare(late) && midPressure != null && latePressure != null
        val canCompareEarlyToMid = hasFullTrend && sufficientForPeriodCompare(early) &&
            sufficientForPeriodCompare(mid) && earlyPressure != null && midPressure != null

        return when {
            canCompareMidToLate && latePressure!! < midPressure!! -> listOf(
                "중반을 지나며 긴장의 기복이 줄었고, 후반으로 갈수록 한결 편안한 흐름이 이어졌습니다.",
                "중반 이후 긴장도가 서서히 낮아지며 후반에는 더 안정된 모습을 보였습니다.",
                "이번 달은 중반을 넘어서며 점차 편안해지는 흐름이 나타났습니다.",
                "중반부터 후반까지 긴장도가 완만하게 낮아지는 경향을 보였습니다.",
                "월 후반으로 갈수록 긴장도가 눈에 띄게 안정되었습니다.",
                "중반 이후의 흐름은 이전보다 한결 차분해졌습니다."
            ).random()

            canCompareEarlyToMid && midPressure!! < earlyPressure!! -> listOf(
                "중반에 접어들며 긴장도가 한층 누그러지는 모습을 보였습니다.",
                "월 중반부터 긴장도가 완만해지는 변화가 나타났습니다.",
                "이번 달은 중반을 지나며 조금씩 편안해지는 흐름을 보였습니다.",
                "중반 무렵부터 긴장이 서서히 풀리는 경향이 있었습니다.",
                "월 초보다 중반의 긴장도가 다소 낮아졌습니다.",
                "중반에 들어서며 이전보다 안정된 상태를 유지했습니다."
            ).random()

            else -> listOf(
                "한 달 내내 큰 굴곡 없이 꾸준한 흐름 속에서 명상이 이어졌습니다.",
                "이번 달 긴장도는 특별한 기복 없이 비교적 일정하게 흘러갔습니다.",
                "시기별로 뚜렷한 변화보다는 전반적으로 고른 흐름을 보였습니다.",
                "이번 달은 시기에 따른 큰 차이 없이 비슷한 흐름으로 이어졌습니다.",
                "긴장도는 한 달 동안 비교적 안정된 수준을 유지했습니다.",
                "시기별 흐름에 큰 굴곡은 없었던 한 달이었습니다."
            ).random()
        }
    }

    private fun timeTrendSentence(early: List<Session>, late: List<Session>, totalSessions: Int): String {
        if (totalSessions < MIN_LIMITED_SESSIONS) {
            return listOf(
                "이번 달에는 기록이 많지 않아 명상 습관의 변화를 판단하기 어렵습니다.",
                "기록 횟수가 적어 명상 시간이나 빈도의 변화를 말씀드리기는 이릅니다.",
                "아직 명상 기록이 충분하지 않아 습관의 변화를 살펴보기는 어렵습니다.",
                "이번 달은 기록이 드물어 시간이나 횟수의 흐름을 짚어내기 어렵습니다.",
                "명상 기록이 많지 않은 달이라 뚜렷한 경향을 말씀드리기 어렵습니다.",
                "이번 달의 기록만으로는 명상 습관의 변화를 판단하기 어렵습니다."
            ).random()
        }

        if (!sufficientForPeriodCompare(early) || !sufficientForPeriodCompare(late)) {
            return listOf(
                "아직 기간별 기록이 충분하지 않아 명상 시간의 변화를 분석하기 어렵습니다.",
                "월 초와 후반의 기록이 고르지 않아 시간의 변화를 비교하기는 어렵습니다.",
                "시기별로 기록이 충분하지 않아 명상 시간의 추세를 판단하기 어렵습니다.",
                "일부 시기의 기록이 적어 명상 시간의 변화를 뚜렷하게 설명드리기 어렵습니다.",
                "기간별 데이터가 고르지 않아 시간의 흐름을 비교하기는 이릅니다.",
                "월 초와 후반 모두에 충분한 기록이 쌓이면 더 정확히 비교해 드릴 수 있습니다."
            ).random()
        }

        val earlyTime = early.sumOf { it.durationSeconds }
        val lateTime = late.sumOf { it.durationSeconds }

        return when {
            lateTime > earlyTime && late.size >= early.size -> listOf(
                "명상 시간과 횟수 모두 후반으로 갈수록 늘어나는 긍정적인 변화를 보였습니다.",
                "월 후반에는 명상 시간과 빈도 모두 늘어난 모습이 나타났습니다.",
                "이번 달은 후반으로 갈수록 명상에 더 많은 시간을 들이신 것으로 보입니다.",
                "명상 횟수와 시간이 함께 늘며 후반부에 더 꾸준한 흐름을 보였습니다.",
                "후반으로 갈수록 명상 시간과 횟수가 고르게 늘어났습니다.",
                "이번 달 후반부에 명상을 더 자주, 더 오래 이어가신 흐름이 보입니다."
            ).random()

            lateTime > earlyTime -> listOf(
                "명상 시간이 점차 늘어나며 더 깊이 있는 명상 습관이 자리잡는 모습이었습니다.",
                "횟수보다는 한 번의 명상 시간이 후반으로 갈수록 길어졌습니다.",
                "이번 달 후반에는 명상에 들이는 시간이 조금씩 늘어났습니다.",
                "명상 시간 자체가 후반으로 갈수록 늘어나는 경향을 보였습니다.",
                "후반부에 접어들며 한 번에 더 오래 명상하시는 모습이 나타났습니다.",
                "명상 횟수는 비슷했지만, 시간은 후반으로 갈수록 늘어났습니다."
            ).random()

            late.size >= early.size -> listOf(
                "명상 횟수가 꾸준히 늘어나며 명상이 일상의 습관으로 자리잡아가고 있습니다.",
                "시간보다는 명상을 실천하는 횟수 자체가 후반으로 갈수록 늘었습니다.",
                "이번 달 후반에는 명상을 더 자주 실천하신 모습이 보입니다.",
                "명상 시간은 비슷했지만, 실천 횟수는 후반으로 갈수록 늘어났습니다.",
                "후반으로 갈수록 명상을 챙기는 빈도가 높아졌습니다.",
                "명상 횟수가 점차 늘며 꾸준함이 더해지는 흐름을 보였습니다."
            ).random()

            else -> listOf(
                "명상 시간과 횟수는 한 달 내내 비교적 안정적으로 유지되었습니다.",
                "이번 달은 시간과 횟수 모두 큰 변화 없이 고르게 이어졌습니다.",
                "명상 시간과 빈도는 월초부터 후반까지 비슷한 수준을 유지했습니다.",
                "특별히 늘거나 줄지 않고, 한 달 내내 비슷한 흐름으로 이어졌습니다.",
                "이번 달의 명상 시간과 횟수는 전반적으로 균형 잡힌 모습이었습니다.",
                "월초와 후반의 차이가 크지 않아, 꾸준한 흐름으로 볼 수 있습니다."
            ).random()
        }
    }

    private fun closingSentence(totalSessions: Int): String = when {
        totalSessions == 1 -> listOf(
            "이번 달의 첫 명상 기록을 남기셨습니다. 다음 달에는 조금 더 자주 만나 뵐 수 있기를 바랍니다.",
            "한 번의 기록이지만, 그 시작이 다음 달의 흐름을 만들어갈 것입니다.",
            "이번 달은 명상을 시작하신 첫걸음으로 기억될 것 같습니다.",
            "짧은 한 번의 기록이었지만, 의미 있는 시작이었습니다.",
            "이번 달의 기록은 하나였지만, 다음 달에는 더 다양한 흐름을 살펴볼 수 있을 것입니다.",
            "첫 기록을 남기신 것만으로도 의미 있는 한 달이었습니다."
        ).random()

        totalSessions < MIN_FULL_SESSIONS -> listOf(
            "아직 기록이 많지는 않지만, 다음 달에는 조금 더 자주 이어가 보시길 바랍니다.",
            "이번 달의 기록들이 다음 달 습관의 밑거름이 될 것입니다.",
            "많지 않은 기록이지만, 그 안에서도 꾸준함의 시작이 엿보입니다.",
            "이번 달은 명상을 조금씩 이어가신 흐름으로 볼 수 있습니다.",
            "기록이 쌓일수록 더 뚜렷한 변화를 함께 살펴볼 수 있을 것입니다.",
            "다음 달에는 조금 더 자주 기록을 남겨 보시는 것도 좋겠습니다."
        ).random()

        else -> listOf(
            "이번 달은 전반적으로 안정적인 명상 습관이 형성되고 있는 흐름을 보여주었습니다.",
            "꾸준한 기록이 쌓이며 명상이 자연스러운 일상으로 자리잡아가고 있습니다.",
            "이번 달의 흐름을 보면 명상이 이미 하나의 습관으로 자리잡은 것 같습니다.",
            "다음 달에도 지금처럼 이어간다면 더욱 편안한 명상 경험을 만들 수 있을 것입니다.",
            "이번 달처럼 꾸준히 이어간다면 그 변화가 더 뚜렷하게 나타날 것입니다.",
            "안정적으로 쌓여가는 기록들이 다음 달의 흐름도 편안하게 만들어 줄 것입니다."
        ).random()
    }

    private fun dayOfMonth(date: String): Int = date.substringAfterLast("-").toIntOrNull() ?: 15

    private fun List<Session>.avgPressureOrNull(): Double? =
        if (isEmpty()) null else map { it.avgPressure }.average()
}
