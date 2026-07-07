package com.newritage.app.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.R
import com.newritage.app.databinding.ActivityMainBinding
import com.newritage.app.ui.main.analysis.AnalysisFragment
import com.newritage.app.ui.main.home.HomeFragment
import com.newritage.app.ui.main.knot.KnotStorageFragment
import com.newritage.app.ui.main.thread.ThreadStorageFragment
import com.newritage.app.ui.settings.SettingsActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav()

        if (savedInstanceState == null) {
            openInitialTab()
        }
    }

    /**
     * 기본은 홈 탭이지만, 다른 화면(예: 측정 화면)의 하단 메뉴에서 넘어온 경우
     * "select_tab" extra에 담긴 탭으로 바로 이동한다.
     */
    private fun openInitialTab() {
        val selectTab = intent.getIntExtra("select_tab", R.id.nav_home)
        if (selectTab == R.id.nav_home) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        } else {
            binding.bottomNav.selectedItemId = selectTab
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, HomeFragment())
                        .commit()
                    true
                }
                R.id.nav_thread -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ThreadStorageFragment())
                        .commit()
                    true
                }
                R.id.nav_knot -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, KnotStorageFragment())
                        .commit()
                    true
                }
                R.id.nav_analysis -> {
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

    /** 홈이 아닌 탭에 있을 때 뒤로가기를 누르면 홈 탭으로 돌아간다. */
    override fun onBackPressed() {
        if (binding.bottomNav.selectedItemId != R.id.nav_home) {
            binding.bottomNav.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressed()
        }
    }
}
