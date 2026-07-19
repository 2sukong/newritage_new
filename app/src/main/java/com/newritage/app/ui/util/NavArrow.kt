package com.newritage.app.ui.util

import android.view.View

/**
 * 좌우 이동 삼각형 화살표(ImageView/ImageButton)의 활성 상태를 토글한다.
 * 비활성이면 클릭이 막히고(alpha로) 회색처럼 흐려져 "그 방향으로 갈 수 없음"을 나타낸다.
 * 분석 페이지와 실/매듭 상세 카탈로그에서 공통으로 쓴다.
 */
fun View.setNavArrowEnabled(enabled: Boolean) {
    isEnabled = enabled
    alpha = if (enabled) 1f else 0.3f
}
