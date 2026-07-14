package com.newritage.app.ui.main.knot.recommend

/**
 * 문장 하나 + 그 문장의 감정 극성 + 가중치를 받아서 매듭별 누적 점수(Map)에 반영하는 채점기.
 *
 * 핵심 규칙: 문장이 긍정/중립이면 [KeywordDictionary.POSITIVE_KEYWORDS]만, 부정이면
 * [KeywordDictionary.NEGATIVE_COMPENSATION_KEYWORDS]만 참조한다. 두 사전을 절대 섞어서 참조하지
 * 않기 때문에 "가족과 싸웠다"처럼 부정 문장에 긍정 키워드('가족')가 섞여 있어도 해당 매듭
 * (가락지매듭)이 잘못 오르지 않는다.
 *
 * 시간복잡도: 매듭 수를 K, 매듭당 평균 키워드 수를 W, 문장 길이를 L이라 하면 문장 하나당
 * O(K * W * L). K, W는 매듭/키워드가 늘어나도 일기 길이와 무관한 고정 크기이므로, 매듭이나
 * 키워드를 추가해도 전체 분석 비용은 일기 분량에 대해 선형을 유지한다.
 */
class KnotScorer {

    fun score(
        sentence: String,
        sentiment: Sentiment,
        weight: Double,
        scores: MutableMap<String, Double>
    ) {
        when (sentiment) {
            Sentiment.POSITIVE -> applyMatches(
                sentence, KeywordDictionary.POSITIVE_KEYWORDS, weight * DIRECT_WEIGHT, scores
            )
            Sentiment.NEUTRAL -> applyMatches(
                sentence, KeywordDictionary.POSITIVE_KEYWORDS, weight * NEUTRAL_WEIGHT, scores
            )
            Sentiment.NEGATIVE -> applyMatches(
                sentence, KeywordDictionary.NEGATIVE_COMPENSATION_KEYWORDS, weight * COMPENSATION_WEIGHT, scores
            )
        }
    }

    private fun applyMatches(
        sentence: String,
        dictionary: Map<String, List<String>>,
        pointsPerHit: Double,
        scores: MutableMap<String, Double>
    ) {
        dictionary.forEach { (knotId, keywords) ->
            val hits = keywords.count { sentence.contains(it) }
            if (hits > 0) {
                scores[knotId] = (scores[knotId] ?: 0.0) + hits * pointsPerHit
            }
        }
    }

    companion object {
        /** 긍정 문장에서 매듭 고유 키워드가 매칭됐을 때의 기본 점수. */
        private const val DIRECT_WEIGHT = 3.0

        /** 중립 문장에서의 매칭 점수. 긍정보다는 약하지만 여전히 주제 관련 신호로 취급한다. */
        private const val NEUTRAL_WEIGHT = 1.0

        /** 부정 문장에서 보정 키워드가 매칭됐을 때의 점수("이 가치가 필요하다" 신호). */
        private const val COMPENSATION_WEIGHT = 2.5
    }
}
