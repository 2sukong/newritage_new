package com.newritage.app.ui.breathing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.newritage.app.ble.BleManager
import com.newritage.app.ble.BreathingCues
import com.newritage.app.data.BreathingTechnique
import com.newritage.app.data.Phase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BreathingSessionState { PREPARING, RUNNING, PAUSED, COMPLETE, CANCELLED }

data class BreathingSessionSnapshot(
    val state: BreathingSessionState,
    val phase: Phase? = null,
    val cycle: Int = 0,
    val totalCycles: Int = 0,
    /** 구간이 (재)시작될 때마다 1씩 증가. 일시정지 후 재개로 같은 구간이 처음부터 다시 시작될 때도
     *  값이 바뀌므로, UI 애니메이션이 "구간 시작"을 정확히 감지해 처음부터 다시 재생할 수 있다. */
    val attempt: Int = 0
)

/**
 * 명상 호흡 가이드 세션 엔진. 판단·타이밍은 전부 여기(앱)서 하고, 기기(BleManager)는 진동
 * 재생만 담당한다. BLE 미연결/끊김 시에도 [BleManager.sendVibration]이 조용히 no-op 처리하므로
 * 화면 가이드(코루틴 진행)는 끊김 없이 계속된다.
 *
 * 일시정지는 진행 중인 delay를 취소하고 STOP 커맨드를 보낸 뒤, 재개 시 현재 구간을
 * 처음부터(마커 재전송 포함) 다시 시작한다 — 남은 시간만큼 이어서 재개하지 않는다.
 */
class BreathingSessionController(
    private val technique: BreathingTechnique,
    minutes: Int
) {
    val totalCycles: Int = (minutes * 60) / technique.cycleSec

    var snapshot by mutableStateOf(
        BreathingSessionSnapshot(BreathingSessionState.PREPARING, totalCycles = totalCycles)
    )
        private set

    private var job: Job? = null
    private var cycleIndex = 1
    private var phaseIndex = 0
    private var attemptCounter = 0

    fun start(scope: CoroutineScope) {
        job = scope.launch { runFromCurrentPosition() }
    }

    fun pause() {
        job?.cancel()
        BleManager.sendStop()
        snapshot = snapshot.copy(state = BreathingSessionState.PAUSED)
    }

    fun resume(scope: CoroutineScope) {
        snapshot = snapshot.copy(state = BreathingSessionState.RUNNING)
        job = scope.launch { runFromCurrentPosition() }
    }

    fun cancel() {
        job?.cancel()
        BleManager.sendStop()
        snapshot = snapshot.copy(state = BreathingSessionState.CANCELLED)
    }

    private suspend fun runFromCurrentPosition() {
        if (totalCycles <= 0) {
            BleManager.sendVibration(BreathingCues.END)
            snapshot = snapshot.copy(state = BreathingSessionState.COMPLETE)
            return
        }

        if (snapshot.state == BreathingSessionState.PREPARING) {
            BleManager.sendVibration(BreathingCues.START)
            delay(PREPARE_DELAY_MS)
            snapshot = snapshot.copy(state = BreathingSessionState.RUNNING)
        }

        val phases = technique.phases
        while (cycleIndex <= totalCycles) {
            while (phaseIndex < phases.size) {
                val phase = phases[phaseIndex]
                BleManager.sendVibration(phase.cue)
                attemptCounter++
                snapshot = snapshot.copy(
                    state = BreathingSessionState.RUNNING,
                    phase = phase,
                    cycle = cycleIndex,
                    attempt = attemptCounter
                )
                delay(phase.durationSec * 1000L)
                phaseIndex++
            }
            phaseIndex = 0
            cycleIndex++
        }

        BleManager.sendVibration(BreathingCues.END)
        snapshot = snapshot.copy(state = BreathingSessionState.COMPLETE)
    }

    companion object {
        private const val PREPARE_DELAY_MS = 4000L
    }
}
