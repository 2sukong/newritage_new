// 🛠 패키지 경로를 SignupActivity와 완벽히 맞춰줍니다
package com.newritage.app.ui.auth

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.newritage.app.R

class SignupCompleteDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_signup_complete, container, false)

        // 배경 투명화 (이것만으로는 크기 고정이 안 될 수 있습니다)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnConfirm = view.findViewById<TextView>(R.id.btnDialogConfirm)
        btnConfirm.setOnClickListener {
            dismiss() // 팝업 닫기
            (activity as? SignupActivity)?.navigateToBaseline()
        }

        return view
    }

    // 🛠 [핵심 추가] 팝업이 화면에 나타날 때 크기와 중앙 정렬을 강제로 고정합니다.
    override fun onResume() {
        super.onResume()

        val dialogWindow = dialog?.window
        if (dialogWindow != null) {
            dialogWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val params = dialogWindow.attributes
            val density = resources.displayMetrics.density

            // 🛠 기존 300에서 260으로 줄여 팝업을 가로로 슬림하게 만듭니다!
            params.width = (260 * density).toInt()
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT

            dialogWindow.attributes = params
        }
    }
}