package com.newritage.app.ui.util

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.min
import kotlin.math.sin

enum class WaveStyle {
    /** 측정 대기 중 (홈 화면, 재생 버튼) — 잔잔한 수면과 천천히 도는 매끄러운 링 */
    IDLE,

    /** 측정 진행 중 — 링이 빠르게 회전하고 이중 물결이 출렁인다 */
    MEASURING,

    COMPLETE
}

/**
 * 홈 화면의 "측정 대기 중" 원과 측정 화면의 "측정 중" 원이 공유하는 게이지 뷰.
 * 바깥에서 안쪽 순서로: 흰색 테두리 한 겹 → 그라데이션 링 → 흰 배경 → 물결/수면.
 * IDLE↔MEASURING 전환은 이 뷰 안에서 startRamp(0→1)로 크로스페이드되어,
 * 화면 전환 애니메이션 없이도 같은 원이 그 자리에서 자연스럽게 모핑되도록 만든다.
 */
class WaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var waveStyle = WaveStyle.MEASURING

    fun setWaveStyle(style: WaveStyle) {
        waveStyle = style
        invalidate()
    }

    private val density = resources.displayMetrics.density

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val waveBackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val waveFrontPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val ringMatrix = Matrix()
    private val clipPath = Path()
    private val wavePath = Path()

    private var fillFactor = 0.46f
    private var targetFillFactor = 0.55f
    private var fillVelocity = 0f
    private var lastSpringTimeMs = 0L

    private var spinAngle = 0f
    private var idleSpinAngle = 0f
    private var startRamp = 1f

    // 서로 배수 관계가 아닌 독립 위상들의 합성으로 불규칙한 물결을 만든다.
    // 각자 자기 자신의 2π 경계에서만 랩되므로 개별적으로는 끊김이 없다.
    private var phaseA = 0f
    private var phaseB = 0f
    private var phaseC = 0f

    fun setPressure(p: Float) {
        targetFillFactor = (p / 80f).coerceIn(0.20f, 0.75f)
        invalidate()
    }

    /** 압력 변화에 따라 수면이 관성을 갖고 자연스럽게(살짝의 출렁임과 함께) 목표 높이를 따라가게 하는 스프링. */
    private fun stepFillSpring() {
        val now = SystemClock.uptimeMillis()
        val dt = if (lastSpringTimeMs == 0L) 0.016f else (now - lastSpringTimeMs).coerceIn(0, 48) / 1000f
        lastSpringTimeMs = now
        val delta = targetFillFactor - fillFactor
        fillVelocity += (delta * FILL_SPRING_STIFFNESS - fillVelocity * FILL_SPRING_DAMPING) * dt
        fillFactor += fillVelocity * dt
    }

    private fun makePhaseAnimator(periodMs: Long, onUpdate: (Float) -> Unit): ValueAnimator =
        ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = periodMs
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                onUpdate(it.animatedValue as Float)
                stepFillSpring()
                invalidate()
            }
        }

    private val phaseAnimatorA = makePhaseAnimator(4200) { phaseA = it }
    private val phaseAnimatorB = makePhaseAnimator(5400) { phaseB = it }
    private val phaseAnimatorC = makePhaseAnimator(6700) { phaseC = it }

    /** 측정 진행상황과 무관하게 링을 빠르게 회전시키는 로딩 모션 (2.6초에 한 바퀴) */
    private val spinAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = MEASURING_SPIN_DURATION_MS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            spinAngle = it.animatedValue as Float
            invalidate()
        }
    }

    /** 대기 상태에서도 항상 아주 천천히 도는 링 모션 (뷰가 화면에 붙어있는 동안 계속 실행) */
    private val idleSpinAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = IDLE_SPIN_DURATION_MS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            idleSpinAngle = it.animatedValue as Float
            invalidate()
        }
    }

    /** 정지 상태에서 측정 시작 시, 회전/물결이 한번에 튀지 않고 서서히 가속되며 자연스럽게 모핑되게 하는 램프업 */
    private val startRampAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = START_RAMP_DURATION_MS
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            startRamp = it.animatedValue as Float
            invalidate()
        }
    }

    private val loopingAnimators
        get() = listOf(phaseAnimatorA, phaseAnimatorB, phaseAnimatorC, spinAnimator)

    /** 애니메이션 시작 (측정 시작 시 호출) */
    fun startWave() {
        loopingAnimators.forEach { if (it.isPaused) it.resume() else if (!it.isStarted) it.start() }
        startRampAnimator.cancel()
        startRamp = 0f
        startRampAnimator.start()
    }

    /** 애니메이션 정지 (측정 종료 시 호출) */
    fun stopWave() {
        loopingAnimators.forEach { if (it.isRunning) it.pause() }
    }

    /** 현재 대기 링의 회전각(0~360). 다른 화면으로 넘어갈 때 이어서 재생하기 위해 사용. */
    fun currentIdleAngle(): Float = idleSpinAngle

    /** 대기 링의 회전각을 지정된 값부터 이어서 재생하도록 시드한다. */
    fun seedIdleAngle(angle: Float) {
        val normalized = ((angle % 360f) + 360f) % 360f
        idleSpinAngle = normalized
        if (!idleSpinAnimator.isStarted) idleSpinAnimator.start()
        idleSpinAnimator.currentPlayTime = (normalized / 360f * IDLE_SPIN_DURATION_MS).toLong()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!idleSpinAnimator.isStarted) idleSpinAnimator.start()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val outerRadius = min(cx, cy) - 2f

        val ringRadius = outerRadius - WHITE_BORDER_WIDTH * density
        val ringStroke = RING_STROKE_WIDTH * density
        val innerRadius = ringRadius - (ringStroke / 2f)

        // ===== 가장 바깥 흰 테두리 한 겹 =====
        canvas.drawCircle(cx, cy, outerRadius, backgroundPaint)

        // ===== 그라데이션 링 =====
        drawRing(canvas, cx, cy, ringRadius, ringStroke)

        // ===== 내부 흰 배경 + 물결/수면 =====
        clipPath.reset()
        clipPath.addCircle(cx, cy, innerRadius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawColor(Color.WHITE)

        canvas.translate(cx - innerRadius, cy - innerRadius)
        val innerSize = innerRadius * 2

        when (waveStyle) {
            WaveStyle.MEASURING -> drawWaterSurface(canvas, innerSize, innerSize, startRamp)
            WaveStyle.COMPLETE -> drawCompleteWave(canvas, innerSize, innerSize)
            WaveStyle.IDLE -> drawWaterSurface(canvas, innerSize, innerSize, 0f)
        }

        canvas.restore()
    }

    /**
     * IDLE(끊김없는 매끄러운 그라데이션·저속 회전)과 MEASURING(기존 3색 그라데이션·고속 회전) 링을
     * startRamp 비율만큼 알파 크로스페이드하며 겹쳐 그려, 화면 전환 없이 자연스럽게 모핑되게 한다.
     */
    private fun drawRing(canvas: Canvas, cx: Float, cy: Float, radius: Float, strokeWidth: Float) {
        val ringRamp = when (waveStyle) {
            WaveStyle.MEASURING -> startRamp
            WaveStyle.COMPLETE -> 1f
            WaveStyle.IDLE -> 0f
        }

        ringPaint.strokeWidth = strokeWidth

        if (ringRamp < 1f) {
            val idleColors = intArrayOf(
                Color.parseColor("#E7EEE2"),
                Color.parseColor("#84AB79"),
                Color.parseColor("#E7EEE2") // 시작=끝 색이 같아 절단면이 생기지 않는다
            )
            val positions = floatArrayOf(0f, 0.5f, 1f)
            val idleShader = SweepGradient(cx, cy, idleColors, positions)
            ringMatrix.reset()
            ringMatrix.setRotate(RING_BASE_ROTATION + idleSpinAngle, cx, cy)
            idleShader.setLocalMatrix(ringMatrix)

            ringPaint.shader = idleShader
            ringPaint.alpha = (255 * (1f - ringRamp)).toInt()
            canvas.drawCircle(cx, cy, radius, ringPaint)
        }

        if (ringRamp > 0f) {
            val measuringColors = intArrayOf(
                Color.parseColor("#84AB79"), // 0%
                Color.parseColor("#F6F8F3"), // 50%
                Color.parseColor("#E0E8DC")  // 100%
            )
            val positions = floatArrayOf(0f, 0.5f, 1f)
            val measuringShader = SweepGradient(cx, cy, measuringColors, positions)
            ringMatrix.reset()
            ringMatrix.setRotate(RING_BASE_ROTATION - spinAngle, cx, cy)
            measuringShader.setLocalMatrix(ringMatrix)

            ringPaint.shader = measuringShader
            ringPaint.alpha = (255 * ringRamp).toInt()
            canvas.drawCircle(cx, cy, radius, ringPaint)
        }
    }

    // -----------------------------
    // Idle ↔ Measuring water surface
    // -----------------------------

    /**
     * rampT=0이면 옅고 요동침 없는 일자 수면(대기 상태), rampT=1이면 기존 이중 물결(측정 중)이 되도록
     * 진폭·투명도를 선형 보간한다. 같은 fillFactor 기준선을 공유해 대기 → 측정 전환이 끊김없이 이어진다.
     */
    private fun drawWaterSurface(canvas: Canvas, w: Float, h: Float, rampT: Float) {
        val topY = h * (1f - fillFactor) - h * 0.12f

        waveBackPaint.shader = LinearGradient(
            0f, topY, 0f, h,
            Color.parseColor("#82A68D"),
            Color.parseColor("#FFFFFF"),
            Shader.TileMode.CLAMP
        )
        waveBackPaint.alpha = lerpInt(IDLE_ALPHA_BACK, MEASURING_ALPHA_BACK, rampT)

        waveFrontPaint.shader = LinearGradient(
            0f, topY, 0f, h,
            Color.parseColor("#D4DCCC"),
            Color.parseColor("#FFFFFF"),
            Shader.TileMode.CLAMP
        )
        waveFrontPaint.alpha = lerpInt(IDLE_ALPHA_FRONT, MEASURING_ALPHA_FRONT, rampT)

        // 뒤쪽 물결: 위상 오프셋이 앞쪽과 어긋나 있어 서로 교차하는 이중 물결을 만든다
        drawWaveFill(
            canvas, w, h,
            Math.PI.toFloat() * 0.9f,
            1f - fillFactor - 0.03f,
            h * 0.17f * rampT,
            0.95,
            waveBackPaint
        )

        drawWaveFill(
            canvas, w, h,
            0f,
            1f - fillFactor,
            h * 0.09f * rampT,
            1.05,
            waveFrontPaint
        )
    }

    private fun lerpInt(from: Int, to: Int, t: Float): Int = (from + (to - from) * t).toInt()

    // -----------------------------
    // Complete
    // -----------------------------

    private fun drawCompleteWave(canvas: Canvas, w: Float, h: Float) {
        val topY = h * (1f - fillFactor) - h * 0.1f

        waveBackPaint.shader = LinearGradient(
            0f, topY, 0f, h,
            Color.parseColor("#DCE8D6"),
            Color.WHITE,
            Shader.TileMode.CLAMP
        )
        waveBackPaint.alpha = 170

        waveFrontPaint.shader = LinearGradient(
            0f, topY, 0f, h,
            Color.parseColor("#A9C79C"),
            Color.WHITE,
            Shader.TileMode.CLAMP
        )
        waveFrontPaint.alpha = 230

        drawWaveFill(canvas, w, h, Math.PI.toFloat(), 1f - fillFactor - 0.02f, h * 0.09f, 0.95, waveBackPaint)
        drawWaveFill(canvas, w, h, 0f, 1f - fillFactor, h * 0.07f, 1.05, waveFrontPaint)
    }

    // -----------------------------
    // Wave Draw
    // -----------------------------

    /**
     * 서로 배수 관계가 아닌 세 위상(phaseA/B/C)을 더하기만 하는 가중합으로 물결 높이를 합성한다.
     * 공간 주파수(freqBase 배율)는 시간축 위상과 무관하므로 비정수를 곱해도 끊김이 생기지 않아
     * 여기서 불규칙한 "진짜 물결" 느낌을 만든다.
     */
    private fun waveHeightAt(x: Float, w: Float, amplitude: Float, freqBase: Double, phaseOffset: Float): Float {
        val t = x / w
        val a = (phaseA + phaseOffset + t * 2 * Math.PI * freqBase).toFloat()
        val b = (phaseB + phaseOffset * 1.3f + 1.7f + t * 2 * Math.PI * freqBase * 1.9).toFloat()
        val c = (phaseC + phaseOffset * 0.6f + 0.6f + t * 2 * Math.PI * freqBase * 0.6).toFloat()
        return amplitude * (0.55f * sin(a) + 0.30f * sin(b) + 0.15f * sin(c))
    }

    private fun drawWaveFill(
        canvas: Canvas,
        w: Float,
        h: Float,
        phaseOffset: Float,
        baselineFraction: Float,
        amplitude: Float,
        frequency: Double,
        paint: Paint
    ) {
        wavePath.reset()

        val waveY = h * baselineFraction
        wavePath.moveTo(0f, waveY)

        val steps = 60
        for (i in 0..steps) {
            val x = w * i / steps
            val y = waveY + waveHeightAt(x, w, amplitude, frequency, phaseOffset)
            wavePath.lineTo(x, y)
        }

        wavePath.lineTo(w, h)
        wavePath.lineTo(0f, h)
        wavePath.close()

        canvas.drawPath(wavePath, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loopingAnimators.forEach { it.cancel() }
        startRampAnimator.cancel()
        idleSpinAnimator.cancel()
    }

    private companion object {
        const val WHITE_BORDER_WIDTH = 5f
        const val RING_STROKE_WIDTH = 4f
        const val RING_BASE_ROTATION = -100f
        const val IDLE_SPIN_DURATION_MS = 50_000L
        const val MEASURING_SPIN_DURATION_MS = 2600L
        const val START_RAMP_DURATION_MS = 650L
        const val FILL_SPRING_STIFFNESS = 180f
        const val FILL_SPRING_DAMPING = 22f
        const val IDLE_ALPHA_BACK = 45
        const val IDLE_ALPHA_FRONT = 35
        const val MEASURING_ALPHA_BACK = 157
        const val MEASURING_ALPHA_FRONT = 135
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFCCCCCC)
@Composable
fun WaveViewPreview() {
    AndroidView(
        factory = { context ->
            WaveView(context).apply {
                // 프리뷰 내부에서 뷰가 쪼그라들지 않도록 꽉 채우는 속성 강제 주입
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )

                setWaveStyle(WaveStyle.MEASURING)
                setPressure(40f)
            }
        },
        // 바깥 Compose 영역의 크기를 200dp로 지정
        modifier = Modifier.size(200.dp)
    )
}
