package com.newritage.app.ui.main.knot.recommend

import com.newritage.app.ui.main.knot.model.KnotInfo
import com.newritage.app.ui.main.knot.model.KnotRepository

/**
 * 월간 매듭 추천 기능의 진입점(facade). 외부(Fragment/ViewModel 등)에서는 이 오브젝트만
 * 알면 되고, 세부 구현([KnotAnalyzer], [SentimentAnalyzer], [KnotScorer], [KeywordDictionary])은
 * 몰라도 된다.
 *
 * 사용 예:
 * ```
 * val recommended: KnotInfo = RecommendationEngine.recommendKnot(monthlyDiaryEntries)
 * ```
 *
 * ### 역할 분리 (유지보수 가이드)
 * - 키워드 추가/수정 → [KeywordDictionary]만 수정.
 * - 감정 판별 로직 개선(예: 형태소 분석기 도입) → [SentimentAnalyzer]만 교체.
 * - 채점 공식(가중치 등) 변경 → [KnotScorer]만 수정.
 * - 문장 분리/가중치 계산 방식 변경 → [KnotAnalyzer]만 수정.
 * - 새 데이터 소스 연동(예: Session.emotion) → 그 소스를 [DiaryEntry]로 매핑하는 코드만 추가.
 * 이 파일(RecommendationEngine)은 위 조각들을 조합해 "최고 점수 매듭 하나"를 고르는 것 외에는
 * 아무 것도 알지 못한다.
 *
 * 전체 시간복잡도는 [KnotAnalyzer]와 동일하게 일기 분량에 대해 선형이며, 마지막 최고점 선택은
 * 매듭 수(K, 상수)에 대해서만 O(K)이므로 무시할 수 있는 수준이다.
 */
object RecommendationEngine {

    private val knotAnalyzer = KnotAnalyzer()

    /** 기본값: 유효한 감정 신호가 전혀 없을 때 반환할 매듭(도래매듭 - "시작"). */
    private const val FALLBACK_KNOT_ID = "dorrae"

    /**
     * 한 달치 감정 기록을 분석해 가장 적합한 매듭 하나를 추천한다.
     *
     * @param entries 이번 달 일기 목록. 순서는 상관없다(내부에서 날짜 기준으로 가중치를 계산한다).
     * @return 가장 점수가 높은 [KnotInfo]. 점수가 있는 매듭이 하나도 없으면(예: 빈 일기, 감정
     *         키워드가 전혀 없는 일기) [FALLBACK_KNOT_ID]를 반환한다.
     */
    fun recommendKnot(entries: List<DiaryEntry>): KnotInfo {
        val scores = knotAnalyzer.analyze(entries)

        val bestKnotId = scores.entries
            .filter { it.value > 0.0 }
            .maxByOrNull { it.value }
            ?.key
            ?: FALLBACK_KNOT_ID

        return KnotRepository.findById(bestKnotId)
            ?: KnotRepository.findById(FALLBACK_KNOT_ID)
            ?: error("KnotRepository에 매듭 데이터가 비어 있습니다.")
    }
}
