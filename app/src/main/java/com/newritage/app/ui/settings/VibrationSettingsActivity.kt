package com.newritage.app.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.ble.VibrationType
import com.newritage.app.databinding.ActivityVibrationSettingsBinding
import com.newritage.app.ui.breathing.BreathingGuideActivity

class VibrationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVibrationSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVibrationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.cardTimer.setOnClickListener { openVibrationList(VibrationType.TIMER) }
        binding.cardTension.setOnClickListener { openVibrationList(VibrationType.TENSION) }
        binding.cardBreathingGuide.setOnClickListener {
            startActivity(Intent(this, BreathingGuideActivity::class.java))
        }
    }

    private fun openVibrationList(type: VibrationType) {
        val intent = Intent(this, VibrationListActivity::class.java)
        intent.putExtra(VibrationListActivity.EXTRA_TYPE, type.name)
        startActivity(intent)
    }
}
