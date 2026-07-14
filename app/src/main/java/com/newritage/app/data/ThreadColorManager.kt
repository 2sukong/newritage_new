package com.newritage.app.data

import com.newritage.app.R

enum class StressState { RELAXED, NORMAL, TENSE }

data class ThreadOption(
    val colorName: String,      // 한글 이름 (DB 저장용, 분석페이지 표시용)
    val drawableRes: Int        // 실제 이미지 리소스 ID
)

object ThreadColorManager {

    // TODO: 실제 drawable 파일명에 맞게 R.drawable.xxx 수정
    private val relaxedThreads = listOf(
        ThreadOption("옥색", R.drawable.thread_ok_saek),
        ThreadOption("비취색", R.drawable.thread_bichwi_saek),
        ThreadOption("벽청색", R.drawable.thread_byeokcheong_saek),
        ThreadOption("쪽색", R.drawable.thread_jjok_saek)
    )
    private val normalThreads = listOf(
        ThreadOption("개나리색", R.drawable.thread_gaenari_saek),
        ThreadOption("유채색", R.drawable.thread_yuchae_saek),
        ThreadOption("송화색", R.drawable.thread_songhwa_saek),
        ThreadOption("버들색", R.drawable.thread_beodeul_saek)
    )
    private val tenseThreads = listOf(
        ThreadOption("동백색", R.drawable.thread_dongbaek_saek),
        ThreadOption("석류색", R.drawable.thread_seokryu_saek),
        ThreadOption("주홍색", R.drawable.thread_juhong_saek),
        ThreadOption("단풍색", R.drawable.thread_danpung_saek)
    )

    fun classifyState(changeRatePercent: Float): StressState = when {
        changeRatePercent >= 20f -> StressState.TENSE
        changeRatePercent <= -20f -> StressState.RELAXED
        else -> StressState.NORMAL
    }

    fun getRandomThreadForState(state: StressState): ThreadOption {
        val list = when (state) {
            StressState.RELAXED -> relaxedThreads
            StressState.NORMAL -> normalThreads
            StressState.TENSE -> tenseThreads
        }
        return list.random()
    }

    // 세션 완료 시점엔 위 getRandomThreadForState로 바로 이미지를 정하므로
    // 이 함수는 "저장된 색상 이름으로 나중에 다시 이미지를 불러올 때"(예: 분석 페이지에서 과거 기록 보여줄 때) 사용
    fun getDrawableByColorName(colorName: String): Int {
        val all = relaxedThreads + normalThreads + tenseThreads
        return all.find { it.colorName == colorName }?.drawableRes
            ?: R.drawable.thread_default
    }
}