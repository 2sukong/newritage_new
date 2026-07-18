package com.newritage.app.data

import com.newritage.app.R

/**
 * 매듭 보관함에서 사용하는 10종 매듭 3D 모델 정보.
 * assetPath는 assets/knots/ 아래 최적화된(Draco 압축) GLB 파일을 가리키며 상세보기의 실시간
 * 3D 렌더에 쓰인다. thumbnailRes는 그리드 썸네일용으로 흰색 재질 기준 한 번 미리 렌더링해 둔
 * 정적 이미지로, 그리드에서는 라이브 3D 대신 이 이미지에 색만 틴트해서 보여준다(그리드는 항상
 * 고정된 각도로만 보이므로 매번 실시간 렌더링할 필요가 없다).
 */
enum class KnotType(val displayName: String, val assetPath: String, val thumbnailRes: Int) {
    GARAKJI("가락지매듭", "knots/garakji.glb", R.drawable.knot_thumb_garakji),
    GUKHWA("국화매듭", "knots/gukhwa.glb", R.drawable.knot_thumb_gukhwa),
    DORAE("도래매듭", "knots/dorae.glb", R.drawable.knot_thumb_dorae),
    MAEHWA("매화매듭", "knots/maehwa.glb", R.drawable.knot_thumb_maehwa),
    SAMJEONGJA("삼정자매듭", "knots/samjeongja.glb", R.drawable.knot_thumb_samjeongja),
    SAENGJJOK("생쪽매듭", "knots/saengjjok.glb", R.drawable.knot_thumb_saengjjok),
    ANGYEONG("안경매듭", "knots/angyeong.glb", R.drawable.knot_thumb_angyeong),
    NABI("나비매듭", "knots/nabi.glb", R.drawable.knot_thumb_nabi),
    BYEONGARI("병아리매듭", "knots/byeongari.glb", R.drawable.knot_thumb_byeongari),
    GAJIBANGSEOK("가지방석매듭", "knots/gajibangseok.glb", R.drawable.knot_thumb_gajibangseok);

    companion object {
        /**
         * TODO: 날짜별로 어떤 매듭 종류를 부여할지 정하는 실제 규칙이 아직 없어,
         * 날짜 문자열을 결정적으로 해싱해 7종 중 하나를 고르는 임시 로직이다.
         * 실제 산출 규칙이 정해지면 이 함수만 교체하면 된다.
         */
        fun forDate(dateStr: String): KnotType {
            val index = (dateStr.hashCode().and(Int.MAX_VALUE)) % entries.size
            return entries[index]
        }

        /**
         * RecommendationEngine이 일기 감정 분석으로 골라낸 매듭 id(KnotRepository 기준 10종)를
         * 각자 전용 3D 모델(KnotType)로 1:1 매핑한다.
         */
        fun fromRecommendationId(id: String): KnotType = when (id) {
            "dorrae" -> DORAE
            "saengjjok" -> SAENGJJOK
            "plum" -> MAEHWA
            "chrysanthemum" -> GUKHWA
            "butterfly" -> NABI
            "samjeongja" -> SAMJEONGJA
            "chick" -> BYEONGARI
            "gaji_bangseok" -> GAJIBANGSEOK
            "ring" -> GARAKJI
            "glasses" -> ANGYEONG
            else -> DORAE
        }
    }
}
