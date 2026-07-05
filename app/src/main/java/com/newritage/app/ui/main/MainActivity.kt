package com.newritage.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.R
import com.newritage.app.databinding.ActivityMainBinding
import com.newritage.app.ui.main.analysis.AnalysisFragment
import com.newritage.app.ui.main.knot.KnotStorageFragment
import com.newritage.app.ui.main.thread.ThreadStorageFragment
import com.newritage.app.ui.measurement.MeasurementActivity
import com.newritage.app.ui.settings.SettingsActivity
import com.newritage.app.ui.util.WaveStyle

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.waveView.setWaveStyle(WaveStyle.IDLE)
        setupHomeButton()
        setupBottomNav()

        // 기본: 홈(측정 시작) 화면
        if (savedInstanceState == null) {
            showHome()
        }
    }

    private fun setupHomeButton() {
        binding.btnStartMeditation.setOnClickListener {
            startActivity(Intent(this, MeasurementActivity::class.java))
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showHome()
                    true
                }
                R.id.nav_thread -> {
                    hideHome()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ThreadStorageFragment())
                        .commit()
                    true
                }
                R.id.nav_knot -> {
                    hideHome()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, KnotStorageFragment())
                        .commit()
                    true
                }
                R.id.nav_analysis -> {
                    hideHome()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AnalysisFragment())
                        .commit()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    @Suppress("DEPRECATION")
                    overridePendingTransition(0, 0)
                    false
                }
                else -> false
            }
        }
    }

    private fun showHome() {
        binding.layoutHome.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE
    }

    private fun hideHome() {
        binding.layoutHome.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE
    }

    /** 홈이 아닌 탭에 있을 때 뒤로가기를 누르면 홈 탭으로 돌아간다. */
    override fun onBackPressed() {
        if (binding.layoutHome.visibility != View.VISIBLE) {
            binding.bottomNav.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressed()
        }
    }
}
