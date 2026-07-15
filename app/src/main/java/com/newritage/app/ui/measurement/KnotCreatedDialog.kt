package com.newritage.app.ui.measurement

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.doOnPreDraw
import com.newritage.app.data.KnotType
import com.newritage.app.databinding.DialogKnotCreatedToastBinding
import com.newritage.app.util.BackdropBlur

/**
 * 매듭 획득 시 화면 중앙에 뜨는 토스트형 팝업. 뒤 화면을 블러 처리해 배경으로 씌우고,
 * 카드 바깥을 탭하면 닫힌다. "바로 보러가기"를 탭하면 닫힌 뒤 [onViewClick]이 실행된다.
 */
class KnotCreatedDialog(
    private val hostActivity: Activity,
    private val knotType: KnotType,
    private val onViewClick: () -> Unit
) : Dialog(hostActivity) {

    private lateinit var binding: DialogKnotCreatedToastBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogKnotCreatedToastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        binding.ivKnotIcon.setImageResource(knotType.thumbnailRes)

        // 카드 바깥 탭 시 닫힘. 카드 자체는 클릭을 소비해 닫히지 않는다.
        binding.dialogRoot.setOnClickListener { dismiss() }
        binding.cardToast.setOnClickListener { }
        binding.tvKnotCreatedCta.setOnClickListener {
            dismiss()
            onViewClick()
        }

        binding.dialogRoot.doOnPreDraw {
            BackdropBlur.applyBottomCropTo(
                source = hostActivity.window.decorView,
                target = binding.ivBackdrop,
                cropHeightPx = binding.dialogRoot.height
            )
        }
    }
}
