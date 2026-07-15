package com.newritage.app.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * 투명 배경 이미지(아이콘/일러스트) 뒤에 그림자를 그려주는 ImageView.
 *
 * 일반적인 View.elevation 그림자는 뷰의 사각형 경계를 따라가므로, 실제 그림이 사각형을
 * 다 채우지 않는 투명 PNG/벡터에는 어색한 사각 그림자가 생긴다. 대신 그려진 드로어블을
 * 알파 채널만 남는 비트맵으로 한 번 래스터화해 그 실루엣을 블러 처리한 뒤 그림자로 쓴다.
 */
class ShadowedImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    var isShadowEnabled: Boolean = false
        set(value) {
            field = value
            rebuildShadow()
        }

    var shadowColor: Int = Color.argb(35, 0, 0, 0)
        set(value) {
            field = value
            rebuildShadow()
        }

    var shadowRadiusPx: Float = resources.displayMetrics.density * 10f
        set(value) {
            field = value
            rebuildShadow()
        }

    var shadowDy: Float = resources.displayMetrics.density * 8f
        set(value) {
            field = value
            invalidate()
        }

    private var shadowBitmap: Bitmap? = null
    private var shadowPadPx: Int = 0
    private val shadowBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildShadow()
    }

    override fun onDraw(canvas: Canvas) {
        shadowBitmap?.let { bmp ->
            canvas.drawBitmap(bmp, -shadowPadPx.toFloat(), -shadowPadPx.toFloat() + shadowDy, shadowBitmapPaint)
        }
        super.onDraw(canvas)
    }

    private fun rebuildShadow() {
        shadowBitmap = null
        val d = drawable
        if (!isShadowEnabled || d == null || width <= 0 || height <= 0) {
            invalidate()
            return
        }

        shadowPadPx = (shadowRadiusPx * 1.5f).toInt().coerceAtLeast(1)
        val bmpW = width + shadowPadPx * 2
        val bmpH = height + shadowPadPx * 2

        // 1) 드로어블을 ALPHA_8 비트맵에 그려 실루엣(알파 채널)만 추출한다.
        val silhouette = runCatching { Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ALPHA_8) }.getOrNull()
            ?: return
        val silhouetteCanvas = Canvas(silhouette)
        silhouetteCanvas.translate(shadowPadPx.toFloat(), shadowPadPx.toFloat())
        val prevBounds = d.copyBounds()
        d.setBounds(0, 0, width, height)
        d.draw(silhouetteCanvas)
        d.bounds = prevBounds

        // 2) 그 실루엣을 지정한 색으로 칠하면서 블러시켜 그림자로 만든다.
        shadowBitmap = runCatching {
            val result = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = shadowColor
                maskFilter = BlurMaskFilter(shadowRadiusPx, BlurMaskFilter.Blur.NORMAL)
            }
            Canvas(result).drawBitmap(silhouette, 0f, 0f, paint)
            result
        }.getOrNull()

        invalidate()
    }
}
