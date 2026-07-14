package com.newritage.app.util

import com.newritage.app.R

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
}