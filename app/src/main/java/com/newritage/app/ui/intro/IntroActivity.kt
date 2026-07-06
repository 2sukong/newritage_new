package com.newritage.app.ui.intro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.databinding.FragmentIntroBinding
import com.newritage.app.ui.measurement.MeasurementActivity

class IntroActivity : AppCompatActivity() {

    private lateinit var binding: FragmentIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            startActivity(Intent(this, MeasurementActivity::class.java))
            finish()
        }
    }
}
