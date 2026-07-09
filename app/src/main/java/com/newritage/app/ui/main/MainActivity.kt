package com.newritage.app.ui.main

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.newritage.app.R
import com.newritage.app.ble.BleManager
import com.newritage.app.databinding.ActivityMainBinding
import com.newritage.app.ui.main.analysis.AnalysisFragment
import com.newritage.app.ui.main.knot.KnotStorageFragment
import com.newritage.app.ui.main.thread.ThreadStorageFragment
import com.newritage.app.ui.measurement.MeasurementActivity
import com.newritage.app.ui.settings.SettingsActivity
import com.newritage.app.ui.util.WaveStyle
import com.newritage.app.util.FeatureFlags

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) {
                BleManager.startScan(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.waveView.setWaveStyle(WaveStyle.IDLE)
        setupHomeButton()
        setupBottomNav()
        connectBle()

        // 기본: 홈(측정 시작) 화면
        if (savedInstanceState == null) {
            showHome()
        }
    }

    private fun connectBle() {
        if (BleManager.hasRequiredPermissions(this)) {
            BleManager.startScan(this)
        } else {
            permissionLauncher.launch(BleManager.requiredPermissions())
        }
    }

    override fun onStart() {
        super.onStart()
        updateConnectionBadge(BleManager.isConnected)
        BleManager.onConnectionChange = { connected -> updateConnectionBadge(connected) }
    }

    override fun onStop() {
        super.onStop()
        BleManager.onConnectionChange = null
    }

    private fun updateConnectionBadge(connected: Boolean) {
        binding.tvConnectionStatus.text = getString(
            if (connected) R.string.connection_connected else R.string.connection_disconnected
        )
        binding.dotConnection.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, if (connected) R.color.primary else R.color.text_hint)
        )
    }

    private fun setupHomeButton() {
        binding.groupIdle.setOnClickListener {
            if (FeatureFlags.REQUIRE_BLE_CONNECTION_TO_START && !BleManager.isConnected) {
                Toast.makeText(this, R.string.connection_required_toast, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, MeasurementActivity::class.java).apply {
                // 대기 링의 회전각을 그대로 이어받아 화면이 바뀌어도 링이 끊김없이 이어서 돌게 한다.
                putExtra(MeasurementActivity.EXTRA_IDLE_RING_ANGLE, binding.waveView.currentIdleAngle())
            }
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        binding.btnHelpHome.setOnClickListener {
            Toast.makeText(this, R.string.feature_in_progress, Toast.LENGTH_SHORT).show()
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
