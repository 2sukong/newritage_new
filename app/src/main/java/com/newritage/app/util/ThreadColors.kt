package com.newritage.app.util

import com.newritage.app.R
import kotlin.math.roundToInt

object ThreadColors {
    data class ThreadColor(
        val nameKr: String,
        val hex: String,
        val level: String,
        val drawableRes: Int
    )

    val LOW = listOf(
        ThreadColor("옥색",   "#8FB9B2", "낮음", R.drawable.thread_ok_saek),
        ThreadColor("비취색", "#67A99A", "낮음", R.drawable.thread_bichwi_saek),
        ThreadColor("벽청색", "#5F8FA8", "낮음", R.drawable.thread_byeokcheong_saek),
        ThreadColor("쪽색",   "#3F5F8C", "낮음", R.drawable.thread_jjok_saek)
    )

    val MEDIUM = listOf(
        ThreadColor("개나리색", "#FFDB4D", "보통", R.drawable.thread_gaenari_saek),
        ThreadColor("유채색",  "#DDBB3F", "보통", R.drawable.thread_yuchae_saek),
        ThreadColor("송화색",  "#C4BB82", "보통", R.drawable.thread_songhwa_saek),
        ThreadColor("버들색",  "#9BAE6D", "보통", R.drawable.thread_beodeul_saek)
    )

    val HIGH = listOf(
        ThreadColor("동백색", "#CB5F67", "높음", R.drawable.thread_dongbaek_saek),
        ThreadColor("석류색", "#A63D4E", "높음", R.drawable.thread_seokryu_saek),
        ThreadColor("주홍색", "#D96A43", "높음", R.drawable.thread_juhong_saek),
        ThreadColor("단풍색", "#B85A3C", "높음", R.drawable.thread_danpung_saek)
    )

    val ALL = LOW + MEDIUM + HIGH

    /**
     * 냉색→온색 12색 스펙트럼 순서(매듭 색상 알고리즘 전용). [LOW]/[MEDIUM]/[HIGH] 버킷 나열
     * 순서와는 다르다 — 이건 실제 색상환상의 위치 기준 정렬이다.
     * 쪽색 → 벽청색 → 비취색 → 옥색 → 버들색 → 송화색 → 유채색 → 개나리색 → 주홍색 → 동백색 → 석류색 → 단풍색
     */
    val SPECTRUM_ORDER: List<ThreadColor> = listOf(
        "쪽색", "벽청색", "비취색", "옥색", "버들색", "송화색",
        "유채색", "개나리색", "주홍색", "동백색", "석류색", "단풍색"
    ).map { name -> ALL.first { it.nameKr == name } }

    /** [SPECTRUM_ORDER] 안에서의 인덱스(0=가장 차가움, 11=가장 뜨거움). 못 찾으면 중간값. */
    fun spectrumIndexOf(hex: String): Int =
        SPECTRUM_ORDER.indexOfFirst { it.hex.equals(hex, true) }.takeIf { it >= 0 } ?: (SPECTRUM_ORDER.size / 2)

    /**
     * 세션 평균 압력(avgPressure)을 baseline 대비 변화율로 계산해
     * -20% 이하: LOW(이완) / -20%~20%: MEDIUM(보통) / 20% 이상: HIGH(긴장)
     * 판단 후, 구간 안에서는 랜덤으로 색을 고른다.
     */
    fun assignColor(avgPressure: Float, baselineOverall: Float): ThreadColor {
        val safeBaseline = baselineOverall.coerceAtLeast(1f)
        val changeRate = ((avgPressure - safeBaseline) / safeBaseline) * 100f

        val bucket = when {
            changeRate >= 20f -> HIGH
            changeRate <= -20f -> LOW
            else -> MEDIUM
        }
        return bucket.random()
    }

    fun findByHex(hex: String): ThreadColor? = ALL.firstOrNull { it.hex.equals(hex, true) }

    fun findByColorName(name: String): ThreadColor? = ALL.firstOrNull { it.nameKr == name }

    /**
     * 여러 hex 색을 RGB 채널별 평균으로 섞은 hex 문자열. 매듭 상세(3D 뷰어)는 공간 클러스터링으로
     * 그 달에 받은 색을 부위별로 섞어 보여주지만, 보관함 썸네일은 정적 이미지에 단색 Modulate 틴트만
     * 입힐 수 있어(성능상 라이브 3D 렌더러를 쓰지 않음) 같은 "섞인 색" 인상을 이 평균색으로 근사한다.
     * 유효한 색이 하나도 없으면 null.
     */
    fun blendHex(hexes: List<String>): String? {
        val colors = hexes.mapNotNull { hex ->
            runCatching { android.graphics.Color.parseColor(hex) }.getOrNull()
        }
        if (colors.isEmpty()) return null
        val r = colors.map { android.graphics.Color.red(it) }.average().roundToInt()
        val g = colors.map { android.graphics.Color.green(it) }.average().roundToInt()
        val b = colors.map { android.graphics.Color.blue(it) }.average().roundToInt()
        return String.format("#%02X%02X%02X", r, g, b)
    }
}