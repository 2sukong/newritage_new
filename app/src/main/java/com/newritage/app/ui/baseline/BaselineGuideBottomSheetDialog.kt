package com.newritage.app.ui.baseline

import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
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
    private val cornerRadiusPx: Float by lazy { 28f * hostActivity.resources.displayMetrics.density }

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
            // 블러 반경이 크면 잘린 비트맵의 가장자리 색(원형 게이지의 흰색 등)이
            // CLAMP 샘플링으로 안쪽까지 번져 모서리 부근이 형체 없는 흰 안개처럼 보인다.
            // 원래 배경의 윤곽(원, 텍스처)이 살아있도록 블러를 약하게 준다.
            BackdropBlur.applyBottomCropTo(
                source = hostActivity.window.decorView,
                target = binding.ivBackdrop,
                cropHeightPx = binding.sheetRoot.height,
                blurRadiusPx = 10f
            )
        }
    }

    /** help.png는 XML에서 바로 참조하지 않고 런타임에 안전하게 로드한다.
     *  손상되었거나 디코딩할 수 없는 리소스여도 시트 전체가 죽지 않도록 방어한다. */
    private fun loadGuideImage() {
        runCatching {
            binding.ivHelpGuide.setImageResource(R.drawable.help_guide)
        }.onFailure {
            Log.e("BaselineGuideSheet", "help.png 로드 실패", it)
        }
    }

    /** RoundedTopFrameLayout이 Canvas.clipPath로 직접 모서리를 잘라내므로 배경/outline은
     *  건드리지 않는다. sheetRoot 자체는 배경을 그리지 않고 자식(backdrop/그라데이션/카드)만
     *  둥근 모양대로 마스킹한다. */
    private fun setupRoundedTopCorners() {
        binding.sheetRoot.topCornerRadiusPx = cornerRadiusPx
    }

    private fun setupBehavior() {
        val bottomSheetView = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return

        // 주의: background를 null로 두면 BottomSheetBehavior가 레이아웃 시점에
        // "배경이 없다"고 판단해 흰색 MaterialShapeDrawable을 자체적으로 다시 씌워버린다.
        // (null이 아니면서 MaterialShapeDrawable도 아닌) 투명 ColorDrawable을 넣어
        // 그 자동 주입 자체를 막는다.
        findViewById<View>(com.google.android.material.R.id.coordinator)?.background = ColorDrawable(Color.TRANSPARENT)
        bottomSheetView.background = ColorDrawable(Color.TRANSPARENT)

        // sheetRoot의 clipPath와는 별개로, Material이 관리하는 design_bottom_sheet 컨테이너
        // 자체에도 동일한 둥근 모서리 클립을 이중으로 걸어 안전망을 둔다.
        bottomSheetView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, (view.height + cornerRadiusPx).toInt(), cornerRadiusPx)
            }
        }
        bottomSheetView.clipToOutline = true

        // 화면 하단 절반만 가리도록 시트 높이를 고정한다.
        val halfScreenHeightPx = hostActivity.resources.displayMetrics.heightPixels / 2
        bottomSheetView.layoutParams = bottomSheetView.layoutParams.apply { height = halfScreenHeightPx }

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
