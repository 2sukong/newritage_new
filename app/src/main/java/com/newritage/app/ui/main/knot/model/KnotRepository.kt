package com.newritage.app.ui.main.knot.model

/** 앱에서 사용하는 매듭 목록. GPT가 추천한 매듭 id로 KnotInfo를 조회할 때도 사용한다. */
object KnotRepository {

    val knots: List<KnotInfo> = listOf(
        KnotInfo(id = "dorrae", name = "도래매듭", meaning = "시작과 연결"),
        KnotInfo(id = "twin_flower", name = "쌍꽃매듭", meaning = "꾸준함"),
        KnotInfo(id = "plum", name = "매화매듭", meaning = "인내와 고결함"),
        KnotInfo(id = "chrysanthemum", name = "국화매듭", meaning = "평안"),
        KnotInfo(id = "butterfly", name = "나비매듭", meaning = "변함없는 사랑과 기쁨, 장수"),
        KnotInfo(id = "samjeongja", name = "삼정자매듭", meaning = "균형과 조화"),
        KnotInfo(id = "chick", name = "병아리매듭", meaning = "귀여움과 생동감"),
        KnotInfo(id = "gaji_bangseok", name = "가지방석매듭", meaning = "안정과 든든함"),
        KnotInfo(id = "ring", name = "가락지매듭", meaning = "사랑과 약속"),
        KnotInfo(id = "glasses", name = "안경매듭", meaning = "연결과 연결성, 장식과 실용")
    )

    fun findById(id: String): KnotInfo? = knots.firstOrNull { it.id == id }
}
