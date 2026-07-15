package com.newritage.app.ui.auth

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.doOnPreDraw
import com.newritage.app.databinding.DialogSignupCompleteBinding
import com.newritage.app.util.BackdropBlur

/**
 * 회원가입 완료 시 뜨는 토스트형 팝업. 매듭 획득 토스트(KnotCreatedDialog)와 같은 디자인으로,
 * 뒤 화면을 블러 처리해 배경으로 씌운다. 카드 바깥을 탭하거나 "확인"을 탭하면 [onConfirm]이 실행된다.
 */
class SignupCompleteDialog(
    private val hostActivity: Activity,
    private val onConfirm: () -> Unit
) : Dialog(hostActivity) {

    private lateinit var binding: DialogSignupCompleteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogSignupCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // 카드 바깥 탭 시에도 확인과 동일하게 다음 화면으로 넘어간다. 카드 자체는 클릭을 소비한다.
        binding.dialogRoot.setOnClickListener { confirmAndDismiss() }
        binding.cardSignupComplete.setOnClickListener { }
        binding.btnDialogConfirm.setOnClickListener { confirmAndDismiss() }

        binding.dialogRoot.doOnPreDraw {
            BackdropBlur.applyBottomCropTo(
                source = hostActivity.window.decorView,
                target = binding.ivBackdrop,
                cropHeightPx = binding.dialogRoot.height
            )
        }
    }

    private fun confirmAndDismiss() {
        dismiss()
        onConfirm()
    }
}
