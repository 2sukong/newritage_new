package com.newritage.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.newritage.app.R
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivitySignupBinding
import com.newritage.app.ui.baseline.BaselineMeasurementActivity

import androidx.lifecycle.lifecycleScope
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.User
import com.newritage.app.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var prefs: UserPreferences
    private lateinit var db: AppDatabase
    private var isDuplicateChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)
        db = AppDatabase.getInstance(this)

        setupUI()
        setupLiveValidation()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnCheckDuplicate.setOnClickListener {
            val id = binding.etUsername.text?.toString()?.trim() ?: ""
            if (id.length < 4 || !USERNAME_PATTERN.matches(id)) {
                Toast.makeText(this, getString(R.string.error_invalid_id), Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    val existingUser = withContext(Dispatchers.IO) {
                        db.userDao().getUserByUsername(id)
                    }
                    if (existingUser != null) {
                        Toast.makeText(this@SignupActivity, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show()
                        isDuplicateChecked = false
                    } else {
                        isDuplicateChecked = true
                        Toast.makeText(this@SignupActivity, "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show()
                        binding.btnCheckDuplicate.text = "확인완료"
                    }
                }
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
            !USERNAME_PATTERN.matches(id) -> {
                Toast.makeText(this, getString(R.string.error_invalid_id), Toast.LENGTH_SHORT).show()
                return
            }
            !isPasswordValid(pw) -> {
                Toast.makeText(this, "비밀번호 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
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

        lifecycleScope.launch {
            val salt = SecurityUtils.generateSalt()
            val hash = SecurityUtils.hashPassword(pw, salt)
            val newUser = User(
                username = id,
                passwordHash = hash,
                salt = salt,
                email = email,
                phone = phone
            )

            try {
                withContext(Dispatchers.IO) {
                    db.userDao().insertUser(newUser)
                }
                // 가입 정보 로컬 저장 (세션 유지용)
                prefs.username = id
                prefs.isLoggedIn = true

                SignupCompleteDialog(this@SignupActivity) { navigateToBaseline() }.show()
            } catch (e: Exception) {
                Toast.makeText(this@SignupActivity, "가입 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 💡 팝업의 '확인' 버튼을 눌렀을 때 Baseline 화면으로 넘어가도록 이 메서드를 호출할 것입니다.
    fun navigateToBaseline() {
        val intent = Intent(this, BaselineMeasurementActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /** 아이디/비밀번호 경고 문구는 입력한 값이 조건을 만족하지 않을 때만 보이도록 한다. */
    private fun setupLiveValidation() {
        binding.etUsername.doOnTextChanged { text, _, _, _ ->
            val value = text?.toString() ?: ""
            binding.tvUsernameWarning.visibility =
                if (value.isNotEmpty() && !USERNAME_PATTERN.matches(value)) View.VISIBLE else View.GONE
        }

        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            val value = text?.toString() ?: ""
            val invalid = value.isNotEmpty() && !isPasswordValid(value)
            binding.tvPasswordUnavailable.visibility = if (invalid) View.VISIBLE else View.GONE
            binding.tvPasswordWarning.visibility = if (invalid) View.VISIBLE else View.GONE
        }
    }

    private fun isPasswordValid(pw: String): Boolean {
        if (pw.length !in 6..20) return false
        var categoryCount = 0
        if (pw.any { it.isUpperCase() }) categoryCount++
        if (pw.any { it.isLowerCase() }) categoryCount++
        if (pw.any { it.isDigit() }) categoryCount++
        if (pw.any { !it.isLetterOrDigit() }) categoryCount++
        return categoryCount >= 2
    }

    private companion object {
        val USERNAME_PATTERN = Regex("^[A-Z][A-Za-z0-9]{3,11}$")
    }
}
