package com.newritage.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityLoginBinding
import com.newritage.app.ui.baseline.BaselineMeasurementActivity
import com.newritage.app.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)

        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnLogin.setOnClickListener { attemptLogin() }

        binding.tvFindId.setOnClickListener {
            Toast.makeText(this, "아이디 찾기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
        binding.tvFindPassword.setOnClickListener {
            Toast.makeText(this, "비밀번호 찾기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
        binding.tvGoSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // SNS 로그인 (미구현 플레이스홀더)
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

        // 실제 서버 연동 없이 로컬 로그인 처리
        prefs.username = id
        prefs.isLoggedIn = true
        prefs.autoLogin = binding.cbAutoLogin.isChecked

        // 기준 압력 미측정 시 BaselineMeasurementActivity로
        val intent = if (!prefs.isBaselineDone) {
            Intent(this, BaselineMeasurementActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
