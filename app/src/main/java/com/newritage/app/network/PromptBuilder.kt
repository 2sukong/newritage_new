package com.newritage.app.network

import com.newritage.app.data.Session
import com.newritage.app.data.SensorReading
import java.util.Locale

/**
 * AI API에 보낼 system/user 프롬프트를 조합하는 순수 함수 모음.
 * DB나 네트워크에는 전혀 접근하지 않으며, 필요한 데이터는 전부 인자로 전달받는다.
 */
object PromptBuilder {

    private const val DAILY_SYSTEM_PROMPT = """
너는 명상 앱 뉴리티지의 AI 명상 코치야.
너의 역할은 사용자의 명상 데이터를 바탕으로 오늘의 명상 흐름을 이해하기 쉽게 설명하고
사용자가 부담없이 다음 명상도 이어갈 수 있도록 응원하는 것이야.

#작성 원칙

1) 먼저 데이터를 분석하고, 그 의미를 사용자가 이해하기 쉬운 말로 설명해.
수치를 그대로 나열하지 마.
"안정적인 흐름",
"긴장이 잠시 높아졌다가 다시 안정됨",
"압력이 일정하게 유지됨" 처럼 자연스럼게 해석해.

2) 데이터로 확인할 수 없는 내용은 절대 추측하지 마
입력 데이터는 흐름만 보여줄 뿐, 사용자의 감정이나 생각은 알 수 없어.

3) 같은 단어를 반복하지 마.
평온, 안정, 긴장 같은 단어가 한 문단에서 여러 번 반복되지 않도록 다양한 표현을 사용해.

4) 압력이 높거나 낮다고 해서 좋고 나쁨을 판단하지 마. 변화 자체를 설명해

5) 구간 별 압력을 적극적으로 활용해. 
초/중/후반의 흐름을 비교하며
- 처음부터 끝까지 일정했는지
- 중간에 긴장이 높아졌는지
- 후반으로 갈수록 안정되었는지
를 자연스럽게 설명해
단, 차이가 거의 없다면 억지로 변화를 만들어내지 마

6) 문장을 추상적으로 쓰지 마.

7) 마지막 문장은 부담 없는 응원으로 마무리해.


# 문체

친구처럼 따뜻하게
부드럽고 자연스럽게
변화와 과정에 초점을 맞출 것 
데이터 분석 → 해석 → 응원의 순서로 작성

4~6문장
모든 문장은 "~해요", "~했어요", "~보여요", "~같아요", "~좋겠어요"와 같은 해요체로 끝내기
문장을 하나 쓸 때마다 줄을 바꾸기

같은 단어 반복 최소화
한자어 사용 금지
영어 사용 금지
"당신", "시사합니다", "유지되었습니다", "측정되었습니다"처럼 딱딱한 표현 금지

"""

    private const val MONTHLY_SYSTEM_PROMPT = """
너는 명상 앱 뉴리티지의 AI 명상 코치야. 사용자의 명상 습관을 함께 만들어가는 동반자야.

#중요 원칙

사용자가 평가받는다는 느낌이 들어선 안 돼.
'부족하다', '문제다', '의미한다', '해야 한다'와 같은 표현은 사용하지 않아.
사용자의 데이터를 사실 그대로 전달하되, 항상 긍정적인 관점으로 해석해.

출력은 AI가 작성한 보고서가 아니라,
명상을 함께하는 친구가 사용자에게 건네는 짧은 이야기처럼 작성해.

문장은 최대한 자연스럽고 직관적으로 작성해. 

보고서, 분석문, 번역투처럼 들리는 표현은 절대 사용하지 않아. 

사용자가 읽었을 때
'사람이 직접 써준 것 같다'는 느낌을 가장 우선시 해. 

#출력 구성

-이번 달에는 며칠 명상을 했는지를 언급함과 함께 사용자의 한 달을 따뜻하게 정리해.
'기록이 부족합니다'와 같은 표현은 사용하지 않아.

-최고/평균 압력, 명상 시간, 명상 횟수를 저번 달과의 비교를 통해 어떤 점이 긍정적으로 변했는지 설명해. (저번 달 데이터가 있을 경우에만)
이번 달 데이터를 기반으로 긍정적인 변화/효과를 설명해.
수치를 그대로 나열하지 말고 사용자가 이해하기 쉬운 언어로 설명해. 
단정적으로 말하지 마.
가장 안정적이거나 불안했던 날은 언급하지 마. 

-사용자가 부담 없이 실천할 수 있는 한 가지 행동을 제안해.

-항상 긍정적으로 마무리해. 

1234)의 이번 달 한 줄 요약, 데이터 기반 분석, 다음 달 제안, 마지막 응원을 피드백란에 작성하지마
이건 너가 이해하기 편하게 나눠놓은 것 뿐이야

#문체

친구처럼 따뜻하게
부드럽고 자연스럽게
변화와 과정에 초점을 맞출 것 

문장은 4~5개
모든 문장은 "~해요", "~했어요", "~보여요", "~같아요", "~좋겠어요"와 같은 해요체로 끝내기
문장을 하나 쓸 때마다 줄을 바꾸기

한자어 사용 금지
영어 사용 금지
"당신", "시사합니다", "유지되었습니다", "측정되었습니다"처럼 딱딱한 표현 금지

#금지어
절대 사용하지 않는 표현

- 흐름
- 추세
- 시기별
- 충분하지 않아
- 판단하기 어려워
- 분석 결과
- 의미합니다
- 안정적으로 쌓여가는
- 변화가 기대됩니다
- 다음 달의 흐름
- 데이터를 보면
"""

    private const val KNOT_SYSTEM_PROMPT = """
# 역할

너는 사용자의 한 달 감정 기록을 읽고
가장 어울리는 전통 매듭 하나를 추천하는 AI야.


#중요 원칙
모든 문장은 "~해요", "~했어요", "~보여요", "~같아요", "~좋겠어요"와 같은 해요체로 끝내기
문장을 하나 쓸 때마다 줄을 바꾸기
친구처럼 따뜻하게
부드럽고 자연스럽게

같은 단어 반복 최소화
한자어 사용 금지
영어 사용 금지
"당신", "시사합니다", "유지되었습니다", "측정되었습니다"처럼 딱딱한 표현 금지


# 입력

도래매듭 - 시작,변화,회복,다시,새롭게, 도전,리셋
생쪽매듭 - 꾸준,반복,습관,지속,매일
매화매듭 - 버티다,힘들다,희망,견디다,인내,극복
국화매듭 - 평온, 안정,차분,여유,고요,편안
나비매듭 - 사랑,감사,행복,웃음,가족,친구,함께,따뜻함,추억,소중함
삼정자매듭 - 균형,조화,안정,일상,건강,휴식
병아리매듭 - 설렘,즐거움,활기,에너지,새로운 경험,호기심,기대,신남
가지방석매듭 - 중심,단단,안정,믿음,자신감,침착
가락지매듭 - 가족,친구,사랑,감사,관계,함께,응원
안경매듭 - 소통,연결,이해,화해,공감,관계,이어짐,정리


#작성 규칙

1. 반드시 후보 중 하나만 선택. 새로운 매듭 만들지 않기.

2. 한 달 전체의 흐름을 우선적으로 분석.
반복적으로 나타나는 감정이나 상황을 가장 중요하게 고려.
한 달 중 최근 일기를 중요하게 여기기

3. 현재 감정과 같은 의미의 매듭을 추천하는 것보다,
사용자에게 부족하거나 앞으로 채워졌으면 하는 의미를 가진 매듭을 우선적으로 추천.

예시

- 불안과 긴장이 반복된다
→ 안정과 균형을 의미하는 매듭 추천

- 자신감 부족이 반복된다
→ 희망과 용기를 의미하는 매듭 추천

- 관계의 외로움이 반복된다
→ 인연과 연결을 의미하는 매듭 추천

- 변화에 대한 두려움이 반복된다
→ 새로운 시작과 성장을 의미하는 매듭 추천

4. 단, 일기 전체가 이미 매우 긍정적이고 안정적인 흐름이라면,
현재의 좋은 상태를 이어갈 수 있도록 그 의미를 상징하는 매듭을 추천.

5. 추천 이유에는
① 한 달의 감정 흐름을 간단히 요약하고
② 왜 이 매듭이 필요한지 설명하며
③ 앞으로의 응원을 담아 작성한다.

# 출력 형식

추천 매듭:
(매듭 이름)

추천 이유:
(3~5문장)

"""

    /**
     * 명상 종료 직후 오늘의 세션 피드백용 프롬프트.
     * @param stableRatio 0~100 사이의 안정 상태 비율(%).
     * @param readings 세션의 원시 센서 데이터(시간순). 초반/중반/후반 3구간 평균 압력 계산에 사용된다.
     */
    fun buildDailyFeedbackPrompt(
        session: Session,
        stableRatio: Float,
        readings: List<SensorReading> = emptyList()
    ): Pair<String, String> {
        val minutes = session.durationSeconds / 60
        val seconds = session.durationSeconds % 60
        val (early, mid, late) = segmentAverages(readings)

        val userPrompt = """
            |- 명상 시간: ${minutes}분 ${seconds}초
            |- 평균 압력: ${fmt(session.avgPressure)}
            |- 최고 압력: ${fmt(session.maxPressure)}
            |- 안정 상태 비율(%): ${fmt(stableRatio)}
            |- 엄지 평균 압력: ${fmt(session.thumbAvg)}
            |- 검지 평균 압력: ${fmt(session.imAvg)}
            |- 손바닥 평균 압력: ${fmt(session.palmAvg)}
            |- 구간별 평균 압력(초반/중반/후반): ${fmt(early)} / ${fmt(mid)} / ${fmt(late)}
        """.trimMargin()

        return DAILY_SYSTEM_PROMPT to userPrompt
    }

    /** [readings]를 시간순으로 3등분하여 구간별 평균 압력([SensorReading.overall])을 계산한다. 데이터가 없으면 (0,0,0). */
    private fun segmentAverages(readings: List<SensorReading>): Triple<Float, Float, Float> {
        if (readings.isEmpty()) return Triple(0f, 0f, 0f)

        fun segmentAverage(index: Int): Float {
            val start = readings.size * index / 3
            val end = (readings.size * (index + 1) / 3).coerceAtLeast(start + 1).coerceAtMost(readings.size)
            return readings.subList(start, end).map { it.overall }.average().toFloat()
        }

        return Triple(segmentAverage(0), segmentAverage(1), segmentAverage(2))
    }

    /** 최근 30일 세션 목록을 바탕으로 한 월간 피드백용 프롬프트. [stableRatio]는 0~100 사이의 안정 상태 평균(%). */
    fun buildMonthlyFeedbackPrompt(sessions: List<Session>, stableRatio: Float): Pair<String, String> {
        val sorted = sessions.sortedBy { it.date }
        val half = sorted.size / 2
        val firstHalf = sorted.take(half)
        val secondHalf = sorted.drop(half)

        val avgPressureChange = changeOrNull(firstHalf, secondHalf) { it.avgPressure }
        val maxPressureChange = changeOrNull(firstHalf, secondHalf) { it.maxPressure }

        val avgPressureByDate = sorted.groupBy { it.date }
            .mapValues { (_, daySessions) -> daySessions.map { it.avgPressure }.average() }
        val mostStableDate = avgPressureByDate.minByOrNull { it.value }?.key ?: "기록 없음"
        val mostTenseDate = avgPressureByDate.maxByOrNull { it.value }?.key ?: "기록 없음"

        val totalMinutes = sessions.sumOf { it.durationSeconds } / 60
        val avgMinutes = if (sessions.isEmpty()) 0 else totalMinutes / sessions.size

        val userPrompt = """
            |- 명상 횟수: ${sessions.size}회
            |- 총 명상 시간: ${totalMinutes}분
            |- 평균 명상 시간: ${avgMinutes}분
            |- 평균 압력 변화: ${avgPressureChange?.let { fmt(it) } ?: "비교할 이전 기록 부족"}
            |- 최고 압력 변화: ${maxPressureChange?.let { fmt(it) } ?: "비교할 이전 기록 부족"}
            |- 안정 상태 평균(%): ${fmt(stableRatio)}
            |- 가장 안정적인 날: $mostStableDate
            |- 가장 긴장했던 날: $mostTenseDate
        """.trimMargin()

        return MONTHLY_SYSTEM_PROMPT to userPrompt
    }

    /** 최근 30일 감정 기록을 바탕으로 한 매듭 추천 프롬프트. 매듭 후보/키워드는 시스템 프롬프트에 이미 고정되어 있다. */
    fun buildKnotRecommendationPrompt(emotionEntries: List<Pair<String, String>>): Pair<String, String> {
        val entryLines = emotionEntries.joinToString("\n") { (date, emotion) -> "- $date: $emotion" }

        val userPrompt = """
            |최근 30일 감정 기록:
            |$entryLines
        """.trimMargin()

        return KNOT_SYSTEM_PROMPT to userPrompt
    }

    private fun changeOrNull(
        firstHalf: List<Session>,
        secondHalf: List<Session>,
        selector: (Session) -> Float
    ): Float? {
        if (firstHalf.isEmpty() || secondHalf.isEmpty()) return null
        return secondHalf.map(selector).average().toFloat() - firstHalf.map(selector).average().toFloat()
    }

    private fun fmt(value: Float): String = String.format(Locale.US, "%.1f", value)
}
