package com.newritage.app
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class WaveHelperView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C2D5C5") // 물결 색상
        style = Paint.Style.FILL
    }

    private val clipPath = Path()
    private var waveOffset = 0f
    private var progressLevel = 0.4f

    init {
        postDelayed(object : Runnable {
            override fun run() {
                waveOffset += 0.1f
                invalidate()
                postDelayed(this, 16)
            }
        }, 16)
    }

    fun setWaveProgress(level: Float) {
        this.progressLevel = level.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = width / 2f
        if (radius <= 0) return

        clipPath.reset()
        clipPath.addCircle(radius, radius, radius, Path.Direction.CW)
        canvas.clipPath(clipPath)

        val wavePath = Path()
        val waveHeight = 15f
        val waveLength = 120f
        val baselineY = height - (height * progressLevel)

        wavePath.moveTo(0f, height.toFloat())
        for (x in 0..width) {
            val y = baselineY + sin((x / waveLength) + waveOffset) * waveHeight
            wavePath.lineTo(x.toFloat(), y)
        }
        wavePath.lineTo(width.toFloat(), height.toFloat())
        wavePath.close()

        canvas.drawPath(wavePath, paint)
    }
}