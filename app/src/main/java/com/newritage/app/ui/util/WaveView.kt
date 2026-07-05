package com.newritage.app.ui.util

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
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
    /** 측정 대기 중 (홈 화면, 재생 버튼) — 링만 정적으로 표시하고 물결은 없음 */
    IDLE,

    /** 측정 진행 중 — 링이 계속 회전하고 이중 물결이 움직인다 */
    MEASURING,

    COMPLETE
}

/**
 * 홈 화면의 "측정 대기 중" 원과 측정 화면의 "측정 중" 원이 공유하는 게이지 뷰.
 * 바깥에서 안쪽 순서로: 흰색 테두리 한 겹 → 그라데이션 링(측정 중엔 계속 회전) → 흰 배경 → (측정 중이면) 이중 물결.
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

    private var phase = 0f
    private var fillFactor = 0.55f
    private var spinAngle = 0f

    fun setPressure(p: Float) {
        fillFactor = (p / 80f).coerceIn(0.20f, 0.75f)
        invalidate()
    }

    /** 물결 위상 애니메이션 (약 4.6초 주기로 순환) */
    private val waveAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
        duration = 4600
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    /** 측정 진행상황과 무관하게 링을 계속 회전시키는 로딩 모션 (2.6초에 한 바퀴) */
    private val spinAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 2600
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            spinAngle = it.animatedValue as Float
            invalidate()
        }
    }

    /** 애니메이션 시작 (측정 시작 시 호출) */
    fun startWave() {
        if (waveAnimator.isPaused) waveAnimator.resume() else if (!waveAnimator.isStarted) waveAnimator.start()
        if (spinAnimator.isPaused) spinAnimator.resume() else if (!spinAnimator.isStarted) spinAnimator.start()
    }

    /** 애니메이션 정지 (측정 종료 시 호출) */
    fun stopWave() {
        if (waveAnimator.isRunning) waveAnimator.pause()
        if (spinAnimator.isRunning) spinAnimator.pause()
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

        // ===== 내부 흰 배경 + 물결 =====
        clipPath.reset()
        clipPath.addCircle(cx, cy, innerRadius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawColor(Color.WHITE)

        canvas.translate(cx - innerRadius, cy - innerRadius)
        val innerSize = innerRadius * 2

        when (waveStyle) {
            WaveStyle.MEASURING -> drawMeasuringWave(canvas, innerSize, innerSize)
            WaveStyle.COMPLETE -> drawCompleteWave(canvas, innerSize, innerSize)
            WaveStyle.IDLE -> Unit // 대기 중에는 물결 없이 흰 배경만
        }

        canvas.restore()
    }

    private fun drawRing(canvas: Canvas, cx: Float, cy: Float, radius: Float, strokeWidth: Float) {
        val colors = intArrayOf(
            Color.parseColor("#84AB79"), // 0%
            Color.parseColor("#F6F8F3"), // 50%
            Color.parseColor("#E0E8DC")  // 100% — 0%와 다른 색이라 여기서 컷이 생김
        )
        val positions = floatArrayOf(0f, 0.5f, 1f)
        val shader = SweepGradient(cx, cy, colors, positions)

        val rotation = RING_BASE_ROTATION + if (waveStyle == WaveStyle.MEASURING) spinAngle else 0f
        ringMatrix.reset()
        ringMatrix.setRotate(rotation, cx, cy)
        shader.setLocalMatrix(ringMatrix)

        ringPaint.shader = shader
        ringPaint.strokeWidth = strokeWidth
        canvas.drawCircle(cx, cy, radius, ringPaint)
    }

    // -----------------------------
    // Measurement
    // -----------------------------

    private fun drawMeasuringWave(canvas: Canvas, w: Float, h: Float) {
        val topY = h * (1f - fillFactor) - h * 0.12f

        waveBackPaint.shader = LinearGradient(
            0f, topY, 0f, h,
            Color.parseColor("#82A68D"),
            Color.parseColor("#FFFFFF"),
            Shader.TileMode.CLAMP
        )
        waveBackPaint.alpha = 157

        waveFrontPaint.shader = LinearGradient(
            0f, topY, 0f, h,
            Color.parseColor("#D4DCCC"),
            Color.parseColor("#FFFFFF"),
            Shader.TileMode.CLAMP
        )
        waveFrontPaint.alpha = 135

        // 뒤쪽 물결: 위상이 앞쪽과 어긋나 있어 서로 교차하는 이중 물결을 만든다
        drawWaveFill(
            canvas, w, h,
            phase * 0.8f + Math.PI.toFloat() * 0.9f,
            1f - fillFactor - 0.03f,
            h * 0.17f,
            0.95,
            waveBackPaint
        )

        drawWaveFill(
            canvas, w, h,
            phase,
            1f - fillFactor,
            h * 0.09f,
            1.05,
            waveFrontPaint
        )
    }

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

        drawWaveFill(canvas, w, h, phase * 0.7f + Math.PI.toFloat(), 1f - fillFactor - 0.02f, h * 0.09f, 0.95, waveBackPaint)
        drawWaveFill(canvas, w, h, phase * 0.9f, 1f - fillFactor, h * 0.07f, 1.05, waveFrontPaint)
    }

    // -----------------------------
    // Wave Draw
    // -----------------------------

    private fun drawWaveFill(
        canvas: Canvas,
        w: Float,
        h: Float,
        phase: Float,
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
            val y = waveY + amplitude * sin((phase + x / w * 2 * Math.PI * frequency).toFloat())
            wavePath.lineTo(x, y)
        }

        wavePath.lineTo(w, h)
        wavePath.lineTo(0f, h)
        wavePath.close()

        canvas.drawPath(wavePath, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator.cancel()
        spinAnimator.cancel()
    }

    private companion object {
        const val WHITE_BORDER_WIDTH = 5f
        const val RING_STROKE_WIDTH = 4f
        const val RING_BASE_ROTATION = -100f
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
