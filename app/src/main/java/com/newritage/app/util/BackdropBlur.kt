package com.newritage.app.util

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.core.view.drawToBitmap

/**
 * 팝업/바텀시트 뒤에 보이는 화면을 캡처해 frosted-glass 스타일 배경으로 씌우는 유틸.
 * RenderEffect 블러는 API 31(S) 이상에서만 실제로 렌더링되며, 그 미만에서는
 * 반투명 흰색 오버레이만 적용된다(요청된 "최대한 구현" 범위).
 */
object BackdropBlur {

    /** [source]의 하단 [cropHeightPx] 영역만 잘라내 [target]에 세팅한다(바텀시트 배경용). */
    fun applyBottomCropTo(source: View, target: ImageView, cropHeightPx: Int, blurRadiusPx: Float = 40f) {
        if (source.width == 0 || source.height == 0 || cropHeightPx <= 0) return
        val full = runCatching { source.drawToBitmap() }.getOrNull() ?: return
        val startY = (full.height - cropHeightPx).coerceIn(0, full.height - 1)
        val height = cropHeightPx.coerceAtMost(full.height - startY)
        val cropped = runCatching { Bitmap.createBitmap(full, 0, startY, full.width, height) }.getOrNull() ?: return
        target.setImageBitmap(cropped)
        applyBlur(target, blurRadiusPx)
    }

    private fun applyBlur(target: ImageView, blurRadiusPx: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            target.setRenderEffect(
                RenderEffect.createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP)
            )
        }
    }
}
