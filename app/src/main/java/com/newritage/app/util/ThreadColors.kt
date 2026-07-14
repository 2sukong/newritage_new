package com.newritage.app.util

object ThreadColors {
    data class ThreadColor(val nameKr: String, val hex: String, val level: String)

    // ratio = 세션 평균 압력 / 기준(초기) 압력 평균. 0.075 단위 12구간, 낮음(한색)→보통(황색)→높음(난색) 순.
    private val JJOK       = ThreadColor("쪽색",   "#3F5F8C", "낮음")
    private val BYEOKCHUNG = ThreadColor("벽청색", "#5F8FA8", "낮음")
    private val BICHUI     = ThreadColor("비취색", "#67A99A", "낮음")
    private val OK         = ThreadColor("옥색",   "#8FB9B2", "낮음")
    private val GAENARI    = ThreadColor("개나리색", "#FFDB4D", "보통")
    private val YUCHAE     = ThreadColor("유채색",  "#DDBB3F", "보통")
    private val SONGHWA    = ThreadColor("송화색",  "#C4BB82", "보통")
    private val BEODEUL    = ThreadColor("버들색",  "#9BAE6D", "보통")
    private val JUHONG     = ThreadColor("주홍색", "#D96A43", "높음")
    private val DANPUNG    = ThreadColor("단풍색", "#B85A3C", "높음")
    private val DONGBAEK   = ThreadColor("동백색", "#CB5F67", "높음")
    private val SEOKRYU    = ThreadColor("석류색", "#A63D4E", "높음")

    val LOW = listOf(JJOK, BYEOKCHUNG, BICHUI, OK)
    val MEDIUM = listOf(GAENARI, YUCHAE, SONGHWA, BEODEUL)
    val HIGH = listOf(JUHONG, DANPUNG, DONGBAEK, SEOKRYU)

    val ALL = LOW + MEDIUM + HIGH

    // ratio 구간 상한(오름차순). 각 구간은 [이전 상한, 이 상한) 이며, 마지막 항목의 색이 상한 이상 전체를 담당.
    private val THRESHOLDS = listOf(
        0.625f to JJOK,
        0.700f to BYEOKCHUNG,
        0.775f to BICHUI,
        0.850f to OK,
        0.925f to GAENARI,
        1.000f to YUCHAE,
        1.075f to SONGHWA,
        1.150f to BEODEUL,
        1.225f to JUHONG,
        1.300f to DANPUNG,
        1.375f to DONGBAEK,
        1.450f to SEOKRYU
    )

    /**
     * 세션 평균 압력을 기준(초기) 압력 평균 대비 비율로 환산해 12색 중 하나를 결정적으로 고른다.
     * 비율이 구간 범위(0.55~1.45) 밖이면 가장 가까운 끝 색으로 고정한다.
     */
    fun assignColor(avgPressure: Float, baselineOverall: Float): ThreadColor {
        val ratio = avgPressure / baselineOverall.coerceAtLeast(1f)
        return THRESHOLDS.firstOrNull { ratio < it.first }?.second ?: SEOKRYU
    }

    fun findByHex(hex: String): ThreadColor? = ALL.firstOrNull { it.hex.equals(hex, true) }
}
