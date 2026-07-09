package com.newritage.app.ui.baseline

import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.util.Log
import android.widget.FrameLayout
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.newritage.app.R
import com.newritage.app.databinding.BottomSheetBaselineGuideBinding
import com.newritage.app.util.BackdropBlur

/**
 * 기준 압력 측정 진입 전 밑에서 올라오는 측정 안내 바텀시트.
 * 뒤에 보이는 준비 화면을 캡처해 frosted-glass 배경으로 씌우고,
 * 아래로 드래그하면 사라지며 본 측정 화면이 그대로 드러난다.
 */
class BaselineGuideBottomSheetDialog(
    private val hostActivity: Activity
) : BottomSheetDialog(hostActivity) {

    private lateinit var binding: BottomSheetBaselineGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = BottomSheetBaselineGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 기본 검정 스크림 제거: 위쪽 준비 화면은 원래 모습 그대로 노출한다.
        window?.setDimAmount(0f)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupRoundedTopCorners()
        setupBehavior()
        loadGuideImage()

        binding.sheetRoot.doOnPreDraw {
            BackdropBlur.applyBottomCropTo(
                source = hostActivity.window.decorView,
                target = binding.ivBackdrop,
                cropHeightPx = binding.sheetRoot.height
            )
        }
    }

    /** help.png는 XML에서 바로 참조하지 않고 런타임에 안전하게 로드한다.
     *  손상되었거나 디코딩할 수 없는 리소스여도 시트 전체가 죽지 않도록 방어한다. */
    private fun loadGuideImage() {
        runCatching {
            binding.ivHelpGuide.setImageResource(R.drawable.help)
        }.onFailure {
            Log.e("BaselineGuideSheet", "help.png 로드 실패", it)
        }
    }

    private fun setupRoundedTopCorners() {
        val radiusPx = 28f * hostActivity.resources.displayMetrics.density
        binding.sheetRoot.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, (view.height + radiusPx).toInt(), radiusPx)
            }
        }
        binding.sheetRoot.clipToOutline = true
    }

    private fun setupBehavior() {
        val bottomSheetView = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return

        // 화면 하단 절반만 가리도록 시트 높이를 고정한다.
        val halfScreenHeightPx = hostActivity.resources.displayMetrics.heightPixels / 2
        bottomSheetView.layoutParams = bottomSheetView.layoutParams.apply { height = halfScreenHeightPx }

        // BottomSheetDialog가 기본으로 씌우는 흰색 MaterialShapeDrawable 배경을 제거한다.
        // 이게 남아있으면 sheetRoot의 둥근 모서리 바깥(클립된 영역)에 흰색이 그대로 비친다.
        bottomSheetView.background = null

        val behavior = BottomSheetBehavior.from(bottomSheetView)
        behavior.isHideable = true
        behavior.skipCollapsed = true
        behavior.peekHeight = halfScreenHeightPx
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(sheetView: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(sheetView: View, slideOffset: Float) = Unit
        })
    }
}
