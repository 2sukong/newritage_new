package com.newritage.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.R
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivitySignupBinding
import com.newritage.app.ui.baseline.BaselineMeasurementActivity

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var prefs: UserPreferences
    private var isDuplicateChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)

        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnCheckDuplicate.setOnClickListener {
            val id = binding.etUsername.text?.toString()?.trim() ?: ""
            if (id.length < 4) {
                Toast.makeText(this, getString(R.string.error_invalid_id), Toast.LENGTH_SHORT).show()
            } else {
                isDuplicateChecked = true
                Toast.makeText(this, "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show()
                binding.btnCheckDuplicate.text = "확인완료"
            }
        }

        binding.btnSendVerification.setOnClickListener {
            val phone = binding.etPhone.text?.toString()?.trim() ?: ""
            if (phone.length < 10) {
                Toast.makeText(this, "올바른 휴대폰 번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "인증번호를 발송했습니다. (테스트: 123456)", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSignup.setOnClickListener { attemptSignup() }
    }

    private fun attemptSignup() {
        val id = binding.etUsername.text?.toString()?.trim() ?: ""
        val pw = binding.etPassword.text?.toString() ?: ""
        val pwConfirm = binding.etPasswordConfirm.text?.toString() ?: ""
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val phone = binding.etPhone.text?.toString()?.trim() ?: ""

        when {
            id.isEmpty() || pw.isEmpty() || pwConfirm.isEmpty() || email.isEmpty() || phone.isEmpty() -> {
                Toast.makeText(this, getString(R.string.error_fill_all), Toast.LENGTH_SHORT).show()
                return
            }
            pw != pwConfirm -> {
                Toast.makeText(this, getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show()
                return
            }
            !isDuplicateChecked -> {
                Toast.makeText(this, "아이디 중복확인을 해주세요.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 가입 정보 로컬 저장
        prefs.username = id
        prefs.isLoggedIn = true

        // 🛠 [변경 포인트] 기존 layout 숨김 처리를 지우고 예쁜 커스텀 다이얼로그 팝업을 띄웁니다!
        val dialog = SignupCompleteDialog()
        dialog.show(supportFragmentManager, "SignupCompleteDialog")
    }

    // 💡 팝업의 '확인' 버튼을 눌렀을 때 Baseline 화면으로 넘어가도록 이 메서드를 호출할 것입니다.
    fun navigateToBaseline() {
        val intent = Intent(this, BaselineMeasurementActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
