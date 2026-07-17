package com.newritage.app.ui.util

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min
import kotlin.math.sin

enum class WaveStyleNew {
    MEASURING,
    COMPLETE
}

class WaveViewNew @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var waveStyle = WaveStyleNew.MEASURING

    fun setWaveStyle(style: WaveStyleNew) {
        waveStyle = style
        invalidate()
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F8FAF8")
        style = Paint.Style.FILL
    }

    private val waveBackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val waveFrontPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val clipPath = Path()
    private val wavePath = Path()
    private val arcRect = RectF()

    private var phase = 0f
    private var fillFactor = 0.50f

    fun setPressure(p: Float) {
        fillFactor = (p / 80f).coerceIn(0.18f, 0.82f)
        invalidate()
    }

    private val animator = ValueAnimator.ofFloat(
        0f,
        (2 * Math.PI).toFloat()
    ).apply {

        duration = 4200

        repeatCount = ValueAnimator.INFINITE

        interpolator = LinearInterpolator()

        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {

        val w = width.toFloat()
        val h = height.toFloat()

        val cx = w / 2f
        val cy = h / 2f

        val radius = min(cx, cy) - 6f

        clipPath.reset()
        clipPath.addCircle(cx, cy, radius, Path.Direction.CW)

        canvas.save()
        canvas.clipPath(clipPath)

        canvas.drawPaint(backgroundPaint)

        when (waveStyle) {

            WaveStyleNew.MEASURING -> drawMeasuringWave(canvas, w, h)

            WaveStyleNew.COMPLETE -> drawCompleteWave(canvas, w, h)

        }

        canvas.restore()
        // ===== 원 테두리 =====

        borderPaint.shader = null
        borderPaint.color = when (waveStyle) {
            WaveStyleNew.MEASURING -> Color.parseColor("#82A68D")
            WaveStyleNew.COMPLETE -> Color.parseColor("#E6E8E2")
        }

        borderPaint.strokeWidth = 2f

        canvas.drawCircle(cx, cy, radius, borderPaint)

        // ===== 상단 진행 아크 =====

        if (waveStyle == WaveStyleNew.MEASURING) {

            borderPaint.color = Color.parseColor("#82A68D")
            borderPaint.strokeWidth = 4f

            arcRect.set(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius
            )

            canvas.drawArc(
                arcRect,
                -90f,
                135f,
                false,
                borderPaint
            )
        }
    }

    // -----------------------------
    // Measurement
    // -----------------------------

    private fun drawMeasuringWave(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {

        waveBackPaint.shader = LinearGradient(
            0f,
            h * 0.35f,
            0f,
            h,
            Color.parseColor("#D4DCCC"),
            Color.WHITE,
            Shader.TileMode.CLAMP
        )

        waveFrontPaint.shader = LinearGradient(
            0f,
            h * 0.30f,
            0f,
            h,
            Color.parseColor("#82A68D"),
            Color.WHITE,
            Shader.TileMode.CLAMP
        )

        waveBackPaint.alpha = 170
        waveFrontPaint.alpha = 255

        drawWave(
            canvas,
            w,
            h,
            phase * 0.75f,
            fillFactor * 0.95f,
            waveBackPaint,
            1.5,
            h * 0.045f
        )

        drawWave(
            canvas,
            w,
            h,
            phase,
            fillFactor,
            waveFrontPaint,
            1.7,
            h * 0.045f
        )
    }

    // -----------------------------
    // Complete
    // -----------------------------

    private fun drawCompleteWave(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        // 뒤쪽 파동 (연한 색상)
        waveBackPaint.shader = LinearGradient(
            0f, h * 0.20f, 0f, h,
            Color.parseColor("#EAF2E6"), // D2E3C8의 아주 연한 버전
            Color.WHITE,
            Shader.TileMode.CLAMP
        )

        // 앞쪽 파동 (D2E3C8)
        waveFrontPaint.shader = LinearGradient(
            0f, h * 0.25f, 0f, h,
            Color.parseColor("#D2E3C8"),
            Color.WHITE,
            Shader.TileMode.CLAMP
        )

        waveBackPaint.alpha = 160
        waveFrontPaint.alpha = 255

        // 두 개의 파동을 그려서 더 풍부한 그라데이션 느낌 구현
        drawWave(
            canvas, w, h, phase * 0.6f, fillFactor + 0.15f,
            waveBackPaint, 1.3, h * 0.04f
        )
        drawWave(
            canvas, w, h, phase * 0.8f, fillFactor + 0.1f,
            waveFrontPaint, 1.5, h * 0.035f
        )
    }

    // -----------------------------
    // Wave Draw
    // -----------------------------

    private fun drawWave(
        canvas: Canvas,
        w: Float,
        h: Float,
        phase: Float,
        fill: Float,
        paint: Paint,
        frequency: Double,
        amplitude: Float
    ) {

        wavePath.reset()

        val waveY = h * (1f - fill)

        wavePath.moveTo(0f, waveY)

        val steps = 60

        for (i in 0..steps) {

            val x = w * i / steps

            val y = waveY +
                    amplitude *
                    sin(
                        (
                                phase +
                                        x / w * 2 * Math.PI * frequency
                                ).toFloat()
                    )

            wavePath.lineTo(x, y)
        }

        wavePath.lineTo(w, h)
        wavePath.lineTo(0f, h)
        wavePath.close()

        canvas.drawPath(wavePath, paint)
    }

    fun startWave() {

        if (animator.isPaused) {
            animator.resume()
        } else if (!animator.isRunning) {
            animator.start()
        }
    }

    fun stopWave() {
        animator.pause()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}

