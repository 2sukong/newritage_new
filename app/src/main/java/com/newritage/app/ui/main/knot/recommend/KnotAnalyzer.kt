package com.newritage.app.ui.main.knot.recommend

/**
 * 한 달치 [DiaryEntry] 목록을 받아 문장 단위로 분리하고, 감정 판별([SentimentAnalyzer]) →
 * 매듭 채점([KnotScorer])을 거쳐 최종 매듭별 누적 점수를 계산한다.
 *
 * 최근 기록에 더 큰 가중치를 주기 위해, 같은 달 안에서 날짜가 늦을수록(넷째 주에 가까울수록)
 * 문장 점수에 곱해지는 가중치를 선형으로 키운다.
 *
 * 시간복잡도: 전체 일기 글자 수를 N, 매듭 수를 K, 매듭당 평균 키워드 수를 W라 하면 전체 분석은
 * O(N * K * W)다. K, W는 매듭/키워드가 늘어나도 일기 분량과 무관한 사실상의 상수이므로,
 * 사전이 아무리 커져도 분석 비용은 일기 길이에 대해 선형을 유지한다(확장에 안전).
 */
class KnotAnalyzer(
    private val sentimentAnalyzer: SentimentAnalyzer = SentimentAnalyzer(),
    private val knotScorer: KnotScorer = KnotScorer()
) {

    /** 매듭 id -> 누적 점수. */
    fun analyze(entries: List<DiaryEntry>): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()

        entries.forEach { entry ->
            val weight = recencyWeight(entry)
            splitIntoSentences(entry.content).forEach { sentence ->
                if (sentence.isBlank()) return@forEach
                val sentiment = sentimentAnalyzer.analyze(sentence)
                knotScorer.score(sentence, sentiment, weight, scores)
            }
        }

        return scores
    }

    /**
     * 문장 분리. 마침표/느낌표/물음표/줄바꿈을 문장 경계로 본다.
     * 정교한 문장 분리기가 아닌 규칙 기반 분리기이며, 필요하면 구분자만 추가하면 된다.
     */
    fun splitIntoSentences(text: String): List<String> =
        text.split(SENTENCE_DELIMITERS)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * 월 중 날짜가 늦을수록 더 큰 가중치를 준다.
     * 매월 1일이면 [MIN_RECENCY_WEIGHT], 그 달의 마지막 날이면 [MAX_RECENCY_WEIGHT]이며
     * 그 사이 날짜는 선형 보간한다. 특정 달의 일부 날짜만 기록이 있어도(예: 20일치만 작성)
     * 달력상의 일자를 기준으로 계산하므로 항상 안정적으로 동작한다.
     */
    private fun recencyWeight(entry: DiaryEntry): Double {
        val daysInMonth = entry.date.lengthOfMonth()
        if (daysInMonth <= 1) return MAX_RECENCY_WEIGHT

        val progress = (entry.date.dayOfMonth - 1).toDouble() / (daysInMonth - 1)
        return MIN_RECENCY_WEIGHT + progress * (MAX_RECENCY_WEIGHT - MIN_RECENCY_WEIGHT)
    }

    companion object {
        private val SENTENCE_DELIMITERS = Regex("[.!?\n]")
        private const val MIN_RECENCY_WEIGHT = 1.0
        private const val MAX_RECENCY_WEIGHT = 2.0
    }
}
