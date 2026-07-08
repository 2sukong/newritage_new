package com.newritage.app.data

import com.newritage.app.ble.BreathingCues

enum class PhaseType { INHALE, HOLD, EXHALE, HOLD2 }

/**
 * @param cue 이 구간이 시작되는 순간 기기에 보낼 진동 효과 번호. [com.newritage.app.ble.BreathingCues] 참고.
 */
data class Phase(val type: PhaseType, val durationSec: Int, val cue: Int)

data class BreathingTechnique(
    val id: String,
    val displayName: String,
    val phases: List<Phase>,
    val note: String? = null
) {
    val cycleSec: Int get() = phases.sumOf { it.durationSec }
}

object BreathingTechniques {
    // 표시 순서: 공명 -> 박스 -> 이완
    val ALL = listOf(
        BreathingTechnique(
            id = "coherent",
            displayName = "공명 호흡",
            phases = listOf(
                Phase(PhaseType.INHALE, 5, BreathingCues.INHALE),
                Phase(PhaseType.EXHALE, 5, BreathingCues.EXHALE)
            )
        ),
        BreathingTechnique(
            id = "box",
            displayName = "박스 호흡",
            phases = listOf(
                Phase(PhaseType.INHALE, 4, BreathingCues.INHALE),
                Phase(PhaseType.HOLD, 4, BreathingCues.HOLD),
                Phase(PhaseType.EXHALE, 4, BreathingCues.EXHALE),
                Phase(PhaseType.HOLD2, 4, BreathingCues.HOLD)
            )
        ),
        BreathingTechnique(
            id = "478",
            displayName = "이완 호흡",
            phases = listOf(
                Phase(PhaseType.INHALE, 4, BreathingCues.INHALE),
                Phase(PhaseType.HOLD, 7, BreathingCues.HOLD),
                Phase(PhaseType.EXHALE, 8, BreathingCues.EXHALE)
            ),
            note = "어지러울 수 있으니 앉거나 누워서 진행하세요. 3분부터 권장합니다."
        )
    )

    val DEFAULT_ID = "coherent"

    fun byId(id: String): BreathingTechnique = ALL.firstOrNull { it.id == id } ?: ALL.first { it.id == DEFAULT_ID }
}
