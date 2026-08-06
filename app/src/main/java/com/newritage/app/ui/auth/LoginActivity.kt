package com.newritage.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityLoginBinding
import com.newritage.app.ui.baseline.BaselineMeasurementActivity
import com.newritage.app.ui.main.MainActivity

import androidx.lifecycle.lifecycleScope
import com.newritage.app.data.AppDatabase
import com.newritage.app.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: UserPreferences
    private lateinit var db: AppDatabase

    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30 * 1000L // 30 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)
        db = AppDatabase.getInstance(this)

        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnLogin.setOnClickListener { attemptLogin() }
// ... (rest of setupUI stays similar)
        binding.tvFindId.setOnClickListener {
            Toast.makeText(this, "아이디 찾기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
        binding.tvFindPassword.setOnClickListener {
            Toast.makeText(this, "비밀번호 찾기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
        binding.tvGoSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.btnGoogle.setOnClickListener {
            Toast.makeText(this, "Google 로그인은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
        binding.btnNaver.setOnClickListener {
            Toast.makeText(this, "네이버 로그인은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
        binding.btnKakao.setOnClickListener {
            Toast.makeText(this, "카카오 로그인은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun attemptLogin() {
        val id = binding.etUsername.text?.toString()?.trim() ?: ""
        val pw = binding.etPassword.text?.toString() ?: ""

        if (id.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, getString(com.newritage.app.R.string.error_fill_all), Toast.LENGTH_SHORT).show()
            return
        }

        // 입력값 검증: SQL Injection 방지 및 형식 체크 (기본적인 필터링)
        if (!id.matches(Regex("^[A-Za-z0-9]+$"))) {
            Toast.makeText(this, "아이디 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                db.userDao().getUserByUsername(id)
            }

            if (user == null) {
                Toast.makeText(this@LoginActivity, "존재하지 않는 사용자입니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val currentTime = System.currentTimeMillis()
            if (user.lockedUntil > currentTime) {
                val remainingSec = (user.lockedUntil - currentTime) / 1000
                Toast.makeText(this@LoginActivity, "계정이 잠겼습니다. ${remainingSec}초 후에 다시 시도하세요.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (SecurityUtils.verifyPassword(pw, user.salt, user.passwordHash)) {
                // 로그인 성공
                val updatedUser = user.copy(failedAttempts = 0, lockedUntil = 0L)
                withContext(Dispatchers.IO) {
                    db.userDao().updateUser(updatedUser)
                }

                prefs.username = id
                prefs.isLoggedIn = true
                prefs.autoLogin = binding.cbAutoLogin.isChecked

                val intent = if (!prefs.isBaselineDone) {
                    Intent(this@LoginActivity, com.newritage.app.ui.baseline.BaselineMeasurementActivity::class.java)
                } else {
                    Intent(this@LoginActivity, com.newritage.app.ui.main.MainActivity::class.java)
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // 로그인 실패
                val newFailedAttempts = user.failedAttempts + 1
                val newLockedUntil = if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {
                    currentTime + LOCKOUT_DURATION_MS
                } else {
                    0L
                }

                val updatedUser = user.copy(
                    failedAttempts = newFailedAttempts,
                    lockedUntil = newLockedUntil
                )
                withContext(Dispatchers.IO) {
                    db.userDao().updateUser(updatedUser)
                }

                if (newLockedUntil > 0) {
                    Toast.makeText(this@LoginActivity, "비밀번호 5회 오류로 계정이 잠겼습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@LoginActivity, "비밀번호가 일치하지 않습니다. ($newFailedAttempts/$MAX_FAILED_ATTEMPTS)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
