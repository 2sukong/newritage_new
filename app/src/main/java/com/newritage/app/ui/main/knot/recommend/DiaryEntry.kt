package com.newritage.app.ui.main.knot.recommend

import java.time.LocalDate

/**
 * 월간 매듭 추천에 사용되는 하루치 감정 기록(일기) 단위.
 *
 * Room의 [com.newritage.app.data.Session] 등 실제 저장 구조와는 분리된 순수 모델이다.
 * 다른 데이터 소스(예: Session.emotion)를 쓰려면 그 소스를 이 타입으로 변환하는 매퍼만
 * 추가하면 되고, 추천 로직([RecommendationEngine] 이하)은 전혀 건드릴 필요가 없다.
 *
 * @property date 기록 날짜. [KnotAnalyzer]가 "최근 기록일수록 더 중요하게" 반영할 때 사용한다.
 * @property content 그날 작성한 감정 기록(일기) 원문.
 */
data class DiaryEntry(
    val date: LocalDate,
    val content: String
)
