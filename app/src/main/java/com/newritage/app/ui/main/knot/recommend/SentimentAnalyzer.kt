package com.newritage.app.ui.main.knot.recommend

/**
 * 문장 하나의 감정 극성(긍정/부정/중립)을 판별하는 규칙 기반 분석기.
 *
 * 형태소 분석기 없이 [KeywordDictionary.GENERAL_POSITIVE_WORDS] / [GENERAL_NEGATIVE_WORDS]
 * (어간 형태로 등록된 일반 감정 어휘)를 문장에서 찾아 개수를 세는 방식으로 동작한다.
 *
 * 부정 표현 처리: 어간 바로 앞에 "안/못", 바로 뒤에 "~지 않/~지 못"이 붙어 있으면 극성을
 * 뒤집는다. (예: "힘들지 않았다" → 부정 어휘가 등장했지만 부정문이므로 오히려 긍정 신호로 처리)
 * 완전한 구문 분석이 아닌 근접 문자열 휴리스틱이므로, 어휘와 멀리 떨어진 부정어나 이중 부정까지는
 * 잡아내지 못한다. 더 정교한 판별이 필요해지면 이 클래스만 교체하면 되고, [KnotAnalyzer]나
 * [KnotScorer] 등 나머지 구조는 영향을 받지 않는다.
 *
 * 시간복잡도: 사전 단어 수를 D, 문장 길이를 L이라 하면 문장 하나당 O(D * L) (indexOf 탐색 포함).
 * D는 사전이 아무리 커져도 매듭 개수와 무관한 고정된 크기이므로, 일기 전체 분석은 여전히
 * 일기 길이에 대해 선형이다.
 */
class SentimentAnalyzer {

    fun analyze(sentence: String): Sentiment {
        var polarity = 0
        KeywordDictionary.GENERAL_POSITIVE_WORDS.forEach { polarity += signalFor(sentence, it, base = 1) }
        KeywordDictionary.GENERAL_NEGATIVE_WORDS.forEach { polarity += signalFor(sentence, it, base = -1) }

        return when {
            polarity > 0 -> Sentiment.POSITIVE
            polarity < 0 -> Sentiment.NEGATIVE
            else -> Sentiment.NEUTRAL
        }
    }

    /** [stem]이 문장에 없으면 0, 있으면 부정 여부에 따라 +[base] 또는 -[base]를 반환한다. */
    private fun signalFor(sentence: String, stem: String, base: Int): Int {
        val index = sentence.indexOf(stem)
        if (index < 0) return 0
        return if (isNegated(sentence, index, stem.length)) -base else base
    }

    private fun isNegated(sentence: String, matchIndex: Int, matchLength: Int): Boolean {
        val before = sentence.substring(maxOf(0, matchIndex - NEGATION_WINDOW), matchIndex)
        val afterEnd = minOf(sentence.length, matchIndex + matchLength + NEGATION_WINDOW)
        val after = sentence.substring(matchIndex + matchLength, afterEnd)

        val prefixNegated = KeywordDictionary.NEGATION_PREFIXES.any { before.trimEnd().endsWith(it) }
        val suffixNegated = KeywordDictionary.NEGATION_SUFFIXES.any { after.startsWith(it) }
        return prefixNegated || suffixNegated
    }

    companion object {
        /** 부정 표지("안", "~지 않" 등)를 찾을 때 어휘 앞/뒤로 살펴볼 문자 수. */
        private const val NEGATION_WINDOW = 3
    }
}
