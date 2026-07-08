package com.newritage.app.ui.breathing

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newritage.app.R
import com.newritage.app.ble.BleManager
import com.newritage.app.data.BreathingTechnique
import com.newritage.app.data.BreathingTechniques
import com.newritage.app.data.PhaseType
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityBreathingGuideBinding
import kotlinx.coroutines.delay

private enum class GuideScreen { SETUP, SESSION, COMPLETE }

private val COLOR_GRADIENT_TOP = Color(0xFF81A68D)
private val COLOR_BACKGROUND = Color(0xFFF5F5F0)
private val COLOR_SURFACE = Color(0xFFFFFFFF)
private val COLOR_PRIMARY = Color(0xFF8B9E7B)
private val COLOR_PRIMARY_LIGHT = Color(0xFFB5C9A5)
private val COLOR_TEXT_PRIMARY = Color(0xFF2D3A2D)
private val COLOR_TEXT_SECONDARY = Color(0xFF5A6B5A)

private val DURATION_OPTIONS = listOf(3, 5, 10)

class BreathingGuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBreathingGuideBinding
    private lateinit var prefs: UserPreferences

    private val bleConnected = mutableStateOf(false)
    private val guideScreen = mutableStateOf(GuideScreen.SETUP)
    private val showCancelConfirm = mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) {
                BleManager.startScan(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreathingGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)
        connectBle()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (guideScreen.value == GuideScreen.SESSION) {
                        showCancelConfirm.value = true
                    } else {
                        finish()
                    }
                }
            }
        )

        binding.composeRoot.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.composeRoot.setContent {
            BreathingGuideScreen(
                prefs = prefs,
                bleConnected = bleConnected.value,
                guideScreen = guideScreen,
                showCancelConfirm = showCancelConfirm,
                onFinish = { finish() }
            )
        }
    }

    private fun connectBle() {
        if (BleManager.hasRequiredPermissions(this)) {
            BleManager.startScan(this)
        } else {
            permissionLauncher.launch(BleManager.requiredPermissions())
        }
    }

    override fun onStart() {
        super.onStart()
        bleConnected.value = BleManager.isConnected
        BleManager.onConnectionChange = { connected -> bleConnected.value = connected }
    }

    override fun onStop() {
        super.onStop()
        BleManager.onConnectionChange = null
    }
}

@Composable
private fun BreathingGuideScreen(
    prefs: UserPreferences,
    bleConnected: Boolean,
    guideScreen: MutableState<GuideScreen>,
    showCancelConfirm: MutableState<Boolean>,
    onFinish: () -> Unit
) {
    var selectedTechniqueId by remember { mutableStateOf(prefs.guideTechniqueId) }
    var selectedMinutes by remember { mutableStateOf(prefs.guideDurationMinutes) }
    var controller by remember { mutableStateOf<BreathingSessionController?>(null) }

    val technique = remember(selectedTechniqueId) { BreathingTechniques.byId(selectedTechniqueId) }
    val activeController = controller

    LaunchedEffect(activeController?.snapshot?.state) {
        if (activeController?.snapshot?.state == BreathingSessionState.COMPLETE) {
            guideScreen.value = GuideScreen.COMPLETE
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(COLOR_GRADIENT_TOP.copy(alpha = 0.18f), COLOR_BACKGROUND))
            )
    ) {
        when (guideScreen.value) {
            GuideScreen.SETUP -> SetupScreen(
                technique = technique,
                minutes = selectedMinutes,
                bleConnected = bleConnected,
                onTechniqueSelect = { id ->
                    selectedTechniqueId = id
                    prefs.guideTechniqueId = id
                    if (id == "478") selectedMinutes = 3
                },
                onMinutesSelect = { minutes ->
                    selectedMinutes = minutes
                    prefs.guideDurationMinutes = minutes
                },
                onStart = {
                    val newController = BreathingSessionController(technique, selectedMinutes)
                    controller = newController
                    guideScreen.value = GuideScreen.SESSION
                }
            )

            GuideScreen.SESSION -> {
                if (activeController != null) {
                    SessionScreen(
                        technique = technique,
                        controller = activeController,
                        bleConnected = bleConnected,
                        onCancelRequest = { showCancelConfirm.value = true }
                    )
                }
            }

            GuideScreen.COMPLETE -> CompleteScreen(onFinish = onFinish)
        }

        if (showCancelConfirm.value) {
            CancelConfirmDialog(
                onConfirm = {
                    controller?.cancel()
                    showCancelConfirm.value = false
                    // TODO: 호흡 가이드 중도 이탈 시 실 지급 여부/기준 정책 미정 — 현재는 아무 보상 없이 종료
                    onFinish()
                },
                onDismiss = { showCancelConfirm.value = false }
            )
        }
    }
}

@Composable
private fun SetupScreen(
    technique: BreathingTechnique,
    minutes: Int,
    bleConnected: Boolean,
    onTechniqueSelect: (String) -> Unit,
    onMinutesSelect: (Int) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.breathing_setup_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = COLOR_TEXT_PRIMARY
        )
        Text(
            text = stringResource(R.string.breathing_setup_desc),
            fontSize = 14.sp,
            color = COLOR_TEXT_SECONDARY,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        if (!bleConnected) {
            Text(
                text = stringResource(R.string.breathing_ble_disconnected),
                fontSize = 12.sp,
                color = COLOR_TEXT_SECONDARY,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            BreathingTechniques.ALL.forEach { item ->
                TechniqueCard(
                    technique = item,
                    isSelected = item.id == technique.id,
                    onClick = { onTechniqueSelect(item.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        technique.note?.let { note ->
            Text(
                text = note,
                fontSize = 12.sp,
                color = COLOR_TEXT_SECONDARY,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(COLOR_SURFACE, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DURATION_OPTIONS.forEach { option ->
                DurationChip(
                    minutes = option,
                    isSelected = option == minutes,
                    onClick = { onMinutesSelect(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = COLOR_PRIMARY),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.breathing_btn_start), color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun TechniqueCard(technique: BreathingTechnique, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) COLOR_PRIMARY_LIGHT.copy(alpha = 0.35f) else COLOR_SURFACE,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = technique.displayName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = COLOR_TEXT_PRIMARY
        )
        Text(
            text = "${technique.cycleSec}초 사이클",
            fontSize = 12.sp,
            color = COLOR_TEXT_SECONDARY,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun DurationChip(minutes: Int, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .background(
                if (isSelected) COLOR_PRIMARY else COLOR_SURFACE,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = stringResource(R.string.breathing_duration_format, minutes),
            color = if (isSelected) Color.White else COLOR_TEXT_PRIMARY,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SessionScreen(
    technique: BreathingTechnique,
    controller: BreathingSessionController,
    bleConnected: Boolean,
    onCancelRequest: () -> Unit
) {
    val snapshot = controller.snapshot
    val scope = rememberCoroutineScope()

    LaunchedEffect(controller) {
        controller.start(scope)
    }

    var remainingSec by remember(controller) {
        mutableStateOf(controller.totalCycles * technique.cycleSec)
    }
    LaunchedEffect(controller) {
        while (remainingSec > 0) {
            delay(1000)
            if (controller.snapshot.state == BreathingSessionState.RUNNING) {
                remainingSec = (remainingSec - 1).coerceAtLeast(0)
            }
        }
    }

    val scale = remember { Animatable(GARDEN_SCALE_CONTRACTED) }
    LaunchedEffect(snapshot.attempt) {
        val phase = snapshot.phase
        if (phase != null) {
            val target = gardenScaleFor(phase.type)
            val startValue = if (phase.type == PhaseType.INHALE || phase.type == PhaseType.HOLD) {
                GARDEN_SCALE_CONTRACTED
            } else {
                GARDEN_SCALE_EXPANDED
            }
            if (phase.type == PhaseType.INHALE || phase.type == PhaseType.EXHALE) {
                scale.snapTo(startValue)
                scale.animateTo(target, tween(durationMillis = phase.durationSec * 1000, easing = LinearEasing))
            } else {
                scale.snapTo(target)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = technique.displayName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = COLOR_TEXT_PRIMARY,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (!bleConnected) {
            Text(
                text = stringResource(R.string.breathing_ble_disconnected),
                fontSize = 12.sp,
                color = COLOR_TEXT_SECONDARY,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size((160 * scale.value).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(COLOR_PRIMARY_LIGHT, COLOR_PRIMARY))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = phaseLabel(snapshot),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (snapshot.state == BreathingSessionState.PREPARING) {
            Text(
                text = stringResource(R.string.breathing_preparing),
                fontSize = 14.sp,
                color = COLOR_TEXT_SECONDARY
            )
        } else {
            Text(
                text = stringResource(R.string.breathing_cycle_format, snapshot.cycle, controller.totalCycles),
                fontSize = 14.sp,
                color = COLOR_TEXT_SECONDARY
            )
        }
        Text(
            text = formatRemaining(remainingSec),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = COLOR_TEXT_PRIMARY,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = {
                    if (controller.snapshot.state == BreathingSessionState.PAUSED) {
                        controller.resume(scope)
                    } else {
                        controller.pause()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (controller.snapshot.state == BreathingSessionState.PAUSED) {
                        stringResource(R.string.breathing_btn_resume)
                    } else {
                        stringResource(R.string.breathing_btn_pause)
                    }
                )
            }
            OutlinedButton(onClick = onCancelRequest, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.breathing_btn_cancel))
            }
        }
    }
}

@Composable
private fun CompleteScreen(onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.breathing_complete_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = COLOR_TEXT_PRIMARY,
            textAlign = TextAlign.Center
        )
        // TODO: 완주 시 실 지급 여부/기준 정책 미정 — 현재는 완료 화면만 노출
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = COLOR_PRIMARY),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.btn_go_home), color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun CancelConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = COLOR_PRIMARY)) {
                Text(stringResource(R.string.ok), color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.breathing_cancel_confirm_title)) },
        text = { Text(stringResource(R.string.breathing_cancel_confirm_desc)) }
    )
}

private const val GARDEN_SCALE_CONTRACTED = 0.6f
private const val GARDEN_SCALE_EXPANDED = 1f

private fun gardenScaleFor(type: PhaseType): Float = when (type) {
    PhaseType.INHALE, PhaseType.HOLD -> GARDEN_SCALE_EXPANDED
    PhaseType.EXHALE, PhaseType.HOLD2 -> GARDEN_SCALE_CONTRACTED
}

@Composable
private fun phaseLabel(snapshot: BreathingSessionSnapshot): String {
    if (snapshot.state == BreathingSessionState.PREPARING) return ""
    return when (snapshot.phase?.type) {
        PhaseType.INHALE -> stringResource(R.string.breathing_phase_inhale)
        PhaseType.EXHALE -> stringResource(R.string.breathing_phase_exhale)
        PhaseType.HOLD, PhaseType.HOLD2 -> stringResource(R.string.breathing_phase_hold)
        null -> ""
    }
}

private fun formatRemaining(totalSeconds: Int): String {
    val min = totalSeconds / 60
    val sec = totalSeconds % 60
    return String.format("%02d:%02d", min, sec)
}
