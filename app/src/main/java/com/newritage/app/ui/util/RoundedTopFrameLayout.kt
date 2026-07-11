package com.newritage.app.ui.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * 위쪽 모서리만 둥글게 잘라내는 FrameLayout.
 *
 * clipToOutline/MaterialShapeDrawable 조합은 BottomSheetBehavior가 배경을 재설정하거나
 * RenderEffect 블러와 겹칠 때 모서리 바깥에 배경색이 그대로 비치는 경우가 있어,
 * Canvas.clipPath로 직접 잘라내 배경(자식 뷰 포함) 렌더링 자체를 확실히 마스킹한다.
 */
class RoundedTopFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var topCornerRadiusPx: Float = 0f
        set(value) {
            field = value
            updateClipPath()
        }

    private val clipPath = Path()
    private val boundsRect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateClipPath()
    }

    private fun updateClipPath() {
        clipPath.reset()
        if (width <= 0 || height <= 0 || topCornerRadiusPx <= 0f) return
        boundsRect.set(0f, 0f, width.toFloat(), height.toFloat())
        val radii = floatArrayOf(
            topCornerRadiusPx, topCornerRadiusPx,
            topCornerRadiusPx, topCornerRadiusPx,
            0f, 0f,
            0f, 0f
        )
        clipPath.addRoundRect(boundsRect, radii, Path.Direction.CW)
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        if (clipPath.isEmpty) {
            super.draw(canvas)
            return
        }
        val saveCount = canvas.save()
        canvas.clipPath(clipPath)
        super.draw(canvas)
        canvas.restoreToCount(saveCount)
    }
}
