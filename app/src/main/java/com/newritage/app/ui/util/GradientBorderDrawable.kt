package com.newritage.app.ui.util

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * 위→아래로 색이 바뀌는 그라데이션 테두리만 그리는 Drawable(내부는 항상 투명).
 * <shape>의 <stroke>는 단색만 지원하므로, LinearGradient 셰이더를 입힌 스트로크 Paint로
 * 라운드 사각형을 직접 그려서 대신한다.
 */
class GradientBorderDrawable(
    private val strokeWidthPx: Float,
    private val cornerRadiusPx: Float,
    private val topColor: Int,
    private val bottomColor: Int
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
    }
    private val rect = RectF()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val inset = strokeWidthPx / 2f
        rect.set(
            bounds.left + inset,
            bounds.top + inset,
            bounds.right - inset,
            bounds.bottom - inset
        )
        paint.shader = LinearGradient(
            0f, rect.top, 0f, rect.bottom,
            topColor, bottomColor,
            Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
