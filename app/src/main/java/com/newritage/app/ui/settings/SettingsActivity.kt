package com.newritage.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.R
import com.newritage.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupDebug()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finishWithSlideBack() }

        binding.groupAccount.root.text = getString(R.string.settings_group_account)
        binding.rowAccountManage.tvLabel.text = getString(R.string.settings_account_manage)
        binding.rowAccountManage.ivIcon.setImageResource(R.drawable.ic_settings_account)
        binding.rowAccountManage.root.setOnClickListener { showNotReady() }

        binding.groupSecurity.root.text = getString(R.string.settings_group_security)
        binding.rowPassword.tvLabel.text = getString(R.string.settings_password)
        binding.rowPassword.ivIcon.setImageResource(R.drawable.ic_settings_lock)
        binding.rowPassword.root.setOnClickListener { showNotReady() }

        binding.groupApp.root.text = getString(R.string.settings_group_app)
        binding.rowNotificationReset.tvLabel.text = getString(R.string.settings_notification_reset)
        binding.rowNotificationReset.ivIcon.setImageResource(R.drawable.ic_settings_refresh)
        binding.rowNotificationReset.root.setOnClickListener { showNotReady() }

        binding.groupPressure.root.text = getString(R.string.settings_group_pressure)
        binding.rowMeasureStandard.tvLabel.text = getString(R.string.settings_measure_standard)
        binding.rowMeasureStandard.ivIcon.setImageResource(R.drawable.ic_settings_tune)
        binding.rowMeasureStandard.root.setOnClickListener { showNotReady() }

        binding.groupVibration.root.text = getString(R.string.settings_group_vibration)
        binding.rowVibrationManage.tvLabel.text = getString(R.string.settings_vibration_manage)
        binding.rowVibrationManage.ivIcon.setImageResource(R.drawable.ic_settings_vibration)
        binding.rowVibrationManage.root.setOnClickListener {
            startActivity(Intent(this, VibrationSettingsActivity::class.java))
        }

        binding.groupHelp.root.text = getString(R.string.settings_group_help)
        binding.rowNotice.tvLabel.text = getString(R.string.settings_notice)
        binding.rowNotice.ivIcon.setImageResource(R.drawable.ic_settings_notice)
        binding.rowNotice.root.setOnClickListener { showNotReady() }

        binding.rowCs.tvLabel.text = getString(R.string.settings_cs)
        binding.rowCs.ivIcon.setImageResource(R.drawable.ic_settings_cs)
        binding.rowCs.root.setOnClickListener { showNotReady() }

        binding.rowGuide.tvLabel.text = getString(R.string.settings_guide)
        binding.rowGuide.ivIcon.setImageResource(R.drawable.ic_settings_guide)
        binding.rowGuide.root.setOnClickListener { showNotReady() }
    }

    private fun setupDebug() {
        binding.groupDebug.root.text = getString(R.string.settings_group_debug)
        binding.rowDebug.tvLabel.text = getString(R.string.settings_debug_manage)
        binding.rowDebug.ivIcon.setImageResource(R.drawable.ic_settings_debug)
        binding.rowDebug.root.setOnClickListener {
            startActivity(Intent(this, DebugSettingsActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun showNotReady() {
        Toast.makeText(this, getString(R.string.feature_in_progress), Toast.LENGTH_SHORT).show()
    }

    private fun finishWithSlideBack() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
