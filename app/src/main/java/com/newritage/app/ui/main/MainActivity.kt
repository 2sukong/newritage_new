package com.newritage.app.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.R
import com.newritage.app.databinding.ActivityMainBinding
import com.newritage.app.ui.main.analysis.AnalysisFragment
import com.newritage.app.ui.main.knot.KnotStorageFragment
import com.newritage.app.ui.main.record.RecordFragment
import com.newritage.app.ui.main.thread.ThreadStorageFragment
import com.newritage.app.ui.measurement.MeasurementActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                R.id.nav_record -> {
                    hideHome()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, RecordFragment())
                        .commit()
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
                    // 설정 페이지 미구현
                    true
                }
                else -> false
            }
        }
    }

    private fun showHome() {
        binding.layoutHome.visibility = android.view.View.VISIBLE
        binding.fragmentContainer.visibility = android.view.View.GONE
    }

    private fun hideHome() {
        binding.layoutHome.visibility = android.view.View.GONE
        binding.fragmentContainer.visibility = android.view.View.VISIBLE
    }

    /** 홈 탭으로 복귀 */
    override fun onBackPressed() {
        if (binding.layoutHome.visibility != android.view.View.VISIBLE) {
            showHome()
            binding.bottomNav.menu.findItem(R.id.nav_record)?.isChecked = false
        } else {
            super.onBackPressed()
        }
    }
}
