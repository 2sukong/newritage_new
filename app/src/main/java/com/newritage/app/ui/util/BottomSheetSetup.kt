package com.newritage.app.ui.util

import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.newritage.app.util.BackdropBlur

private const val OVERLAY_MAX_ALPHA = 0.35f
private const val OPEN_FADE_DURATION_MS = 180L

/**
 * BottomSheetDialog를 화면 높이의 [heightFraction] 비율로 고정하고, 스와이프다운(또는
 * 반환된 behavior를 STATE_HIDDEN으로 바꿔서)으로 닫히는 시트로 구성한다.
 * BaselineGuideBottomSheetDialog에서 쓰던 Material 내부 배경 투명화 + outlineProvider
 * 마스킹 트릭을 공용 헬퍼로 뽑아낸 것.
 *
 * 시트가 덮지 않는 나머지 가장자리에는 뒤 화면을 블러 처리한 캡처를 깔아 뿌연 효과를 준다.
 * 이 배경 레이어는 시트와 완전히 분리된 별도 뷰라 위치 이동 애니메이션이 전혀 없다: 열릴 때는
 * 그 자리에서 짧게 페이드인하고, 닫힐 때는 시트가 실제로 내려가는 슬라이드 진행도
 * ([BottomSheetBehavior.BottomSheetCallback.onSlide])에 맞춰 알파가 같이 줄어든다.
 *
 * 호출자는 반환된 behavior의 state를 STATE_HIDDEN으로 바꿔서 닫아야 이 슬라이드 연동
 * 페이드아웃이 재생된다(= dismiss()를 바로 부르면 물리적 슬라이드 없이 즉시 닫힌다).
 */
fun BottomSheetDialog.configureFixedHeightSheet(
    hostActivity: Activity,
    heightFraction: Float,
    cornerRadiusPx: Float
): BottomSheetBehavior<FrameLayout> {
    val bottomSheetView = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)!!
    val coordinator = findViewById<CoordinatorLayout>(com.google.android.material.R.id.coordinator)

    // 주의: background를 null로 두면 BottomSheetBehavior가 배경이 없다고 판단해 흰색
    // MaterialShapeDrawable을 자체적으로 다시 씌워버린다. 투명 ColorDrawable로 그 자동 주입을 막는다.
    coordinator?.background = ColorDrawable(Color.TRANSPARENT)
    bottomSheetView.background = ColorDrawable(Color.TRANSPARENT)

    var ivBackdrop: ImageView? = null
    var overlayWhite: View? = null

    if (coordinator != null) {
        window?.setDimAmount(0f)

        ivBackdrop = ImageView(hostActivity).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            alpha = 0f
            layoutParams = CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        coordinator.addView(ivBackdrop, 0)

        overlayWhite = View(hostActivity).apply {
            setBackgroundColor(Color.WHITE)
            alpha = 0f
            layoutParams = CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        coordinator.addView(overlayWhite, 1)

        // 최초로 열릴 때의 슬라이드는 뷰가 아직 붙기 전에 상태가 정해져 onSlide가 오지 않을
        // 수 있어(레이아웃 시점 문제), 직접 캡처 직후 짧게 페이드인시킨다. 배경 자체는
        // 위치를 전혀 옮기지 않고 그 자리에서만 흐려진다.
        coordinator.doOnPreDraw {
            BackdropBlur.applyBottomCropTo(
                source = hostActivity.window.decorView,
                target = ivBackdrop,
                cropHeightPx = coordinator.height
            )
            ivBackdrop.animate().alpha(1f).setDuration(OPEN_FADE_DURATION_MS).start()
            overlayWhite.animate().alpha(OVERLAY_MAX_ALPHA).setDuration(OPEN_FADE_DURATION_MS).start()
        }
    }

    bottomSheetView.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, (view.height + cornerRadiusPx).toInt(), cornerRadiusPx)
        }
    }
    bottomSheetView.clipToOutline = true

    val heightPx = (hostActivity.resources.displayMetrics.heightPixels * heightFraction).toInt()
    bottomSheetView.layoutParams = bottomSheetView.layoutParams.apply { height = heightPx }

    val behavior = BottomSheetBehavior.from(bottomSheetView)
    behavior.isHideable = true
    behavior.skipCollapsed = true
    behavior.peekHeight = heightPx
    behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(sheetView: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                ivBackdrop?.alpha = 0f
                overlayWhite?.alpha = 0f
                this@configureFixedHeightSheet.dismiss()
            }
        }

        override fun onSlide(sheetView: View, slideOffset: Float) {
            // slideOffset: 숨김(-1) ~ 펼침(1). 시트가 실제로 내려가는(또는 올라오는) 만큼,
            // 배경 레이어는 위치 이동 없이 같은 진행도로 알파만 줄어들거나(늘어난다).
            val progress = ((slideOffset + 1f) / 2f).coerceIn(0f, 1f)
            ivBackdrop?.alpha = progress
            overlayWhite?.alpha = progress * OVERLAY_MAX_ALPHA
        }
    })
    behavior.state = BottomSheetBehavior.STATE_EXPANDED

    return behavior
}
