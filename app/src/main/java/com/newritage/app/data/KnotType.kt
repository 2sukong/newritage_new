package com.newritage.app.data

/**
 * 매듭 보관함에서 사용하는 7종 매듭 3D 모델 정보.
 * assetPath는 assets/knots/ 아래 최적화된(Draco 압축) GLB 파일을 가리킨다.
 */
enum class KnotType(val displayName: String, val assetPath: String) {
    GARAKJI("가락지매듭", "knots/garakji.glb"),
    GUKHWA("국화매듭", "knots/gukhwa.glb"),
    DORAE("도래매듭", "knots/dorae.glb"),
    MAEHWA("매화매듭", "knots/maehwa.glb"),
    SAMJEONGJA("삼정자매듭", "knots/samjeongja.glb"),
    SAENGJJOK("생쪽매듭", "knots/saengjjok.glb"),
    ANGYEONG("안경매듭", "knots/angyeong.glb");

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
    }
}
