package com.newritage.app.ui.main

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.doOnPreDraw
import com.newritage.app.R
import com.newritage.app.databinding.DialogHelpImageBinding
import com.newritage.app.util.BackdropBlur

/**
 * 도움말 이미지(help.png)를 화면 중앙에 크게 띄우는 풀스크린 팝업.
 * 뒤에 보이는 화면을 캡처해 frosted-glass 배경으로 씌우고, 이미지 바깥 가장자리를
 * 터치하면 닫힌다.
 */
class HelpImageDialog(private val hostActivity: Activity) : Dialog(hostActivity) {

    private lateinit var binding: DialogHelpImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogHelpImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window?.setWindowAnimations(R.style.Animation_HelpImageDialog)

        loadHelpImage()

        // 가장자리(배경) 터치 시 닫기. 이미지 자체는 클릭을 소비해 닫히지 않는다.
        binding.helpDialogRoot.setOnClickListener { dismiss() }
        binding.ivHelpImage.setOnClickListener { }

        binding.helpDialogRoot.doOnPreDraw {
            BackdropBlur.applyBottomCropTo(
                source = hostActivity.window.decorView,
                target = binding.ivBackdrop,
                cropHeightPx = binding.helpDialogRoot.height
            )
        }
    }

    /** help.png는 XML에서 바로 참조하지 않고 런타임에 안전하게 로드한다. */
    private fun loadHelpImage() {
        runCatching {
            binding.ivHelpImage.setImageResource(R.drawable.help)
        }.onFailure {
            Log.e("HelpImageDialog", "help.png 로드 실패", it)
        }
    }
}
