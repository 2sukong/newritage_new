package com.newritage.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.R
import com.newritage.app.data.UserPreferences
import com.newritage.app.ui.auth.StartActivity
import com.newritage.app.ui.baseline.BaselineMeasurementActivity
import com.newritage.app.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefs = UserPreferences(this)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = when {
                !prefs.isOnboardingDone -> Intent(this, OnboardingActivity::class.java)
                !prefs.isLoggedIn -> Intent(this, StartActivity::class.java)
                !prefs.isBaselineDone -> Intent(this, BaselineMeasurementActivity::class.java)
                else -> Intent(this, MainActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 1500L)
    }
}
