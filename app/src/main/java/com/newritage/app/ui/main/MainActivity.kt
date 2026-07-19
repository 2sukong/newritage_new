package com.newritage.app.ui.main

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.newritage.app.R
import com.newritage.app.ble.BleManager
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.KnotType
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityMainBinding
import com.newritage.app.ui.main.analysis.AnalysisFragment
import com.newritage.app.ui.main.knot.KnotStorageFragment
import com.newritage.app.ui.main.knot.recommend.DiaryEntry
import com.newritage.app.ui.main.knot.recommend.RecommendationEngine
import com.newritage.app.ui.main.thread.ThreadStorageFragment
import com.newritage.app.ui.measurement.KnotCreatedDialog
import com.newritage.app.ui.measurement.MeasurementActivity
import com.newritage.app.ui.settings.SettingsActivity
import com.newritage.app.ui.util.WaveStyle
import com.newritage.app.util.DebugDataSeeder
import com.newritage.app.util.DevClock
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private enum class MeasurementMode { AUTONOMOUS, GUIDE }

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: UserPreferences
    private var selectedMode = MeasurementMode.AUTONOMOUS
    private var currentTabId = R.id.nav_home

    /** 하단 탭바에 보이는 순서(왼쪽→오른쪽). 탭 전환 애니메이션의 방향을 정하는 데 쓴다. */
    private val tabOrder = listOf(R.id.nav_thread, R.id.nav_knot, R.id.nav_home, R.id.nav_analysis, R.id.nav_settings)

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
        prefs = UserPreferences(this)

        binding.waveView.setWaveStyle(WaveStyle.IDLE)
        setupHomeButton()
        setupBottomNav()
        setupDevDateBar()
        connectBle()
        lifecycleScope.launch { DebugDataSeeder.seedIfEnabled(this@MainActivity) }

        // 기본: 홈(측정 시작) 화면. 매듭 획득 토스트의 "바로 보러가기"로 들어온 경우 매듭 보관함 탭으로.
        if (savedInstanceState == null) {
            if (intent.getBooleanExtra(EXTRA_NAVIGATE_TO_KNOT, false)) {
                binding.bottomNav.selectedItemId = R.id.nav_knot
            } else {
                binding.layoutHome.visibility = View.VISIBLE
                binding.fragmentContainer.visibility = View.GONE
                binding.bottomNav.selectedItemId = R.id.nav_home
            }
        }
    }

    // ── 시연용 개발자 날짜바 ──────────────────────────────────────────

    /** 하단 개발자 날짜바: 임시 "오늘"을 하루씩 앞뒤로 넘긴다. 매월 1일로 넘어가면 매듭 팝업을 띄운다. */
    private fun setupDevDateBar() {
        applyDevDateBarVisibility()
        renderDevDate()
        binding.btnDevPrevDay.setOnClickListener {
            prefs.devDateOffsetDays -= 1
            renderDevDate()
        }
        binding.btnDevNextDay.setOnClickListener {
            val before = DevClock.today(prefs)
            prefs.devDateOffsetDays += 1
            val after = DevClock.today(prefs)
            renderDevDate()
            // 다음날로 넘겨 매월 1일이 되면(=한 달이 새로 시작되면) 방금 끝난 달의 매듭을 준다.
            if (after.dayOfMonth == 1) {
                showMonthlyKnotPopup(before)
            }
        }
    }

    /** 디버그 토글([UserPreferences.debugDateBarVisible])에 따라 날짜바를 보이거나 숨긴다. */
    private fun applyDevDateBarVisibility() {
        binding.devDateBar.visibility = if (prefs.debugDateBarVisible) View.VISIBLE else View.GONE
    }

    private fun renderDevDate() {
        binding.tvDevDate.text = getString(
            R.string.dev_date_format,
            DevClock.today(prefs).format(DEV_DATE_DISPLAY)
        )
    }

    /** [completedMonthDate]가 속한 달의 일기(emotion)를 분석해 그 달의 매듭을 팝업으로 보여준다. */
    private fun showMonthlyKnotPopup(completedMonthDate: LocalDate) {
        lifecycleScope.launch {
            val yearMonth = completedMonthDate.format(DEV_MONTH)
            val dao = AppDatabase.getInstance(this@MainActivity).sessionDao()
            val diaryEntries = dao.getSessionsByMonth(yearMonth)
                .filter { it.emotion.isNotBlank() }
                .map { DiaryEntry(LocalDate.parse(it.date), it.emotion) }
            val knotType = if (diaryEntries.isNotEmpty()) {
                KnotType.fromRecommendationId(RecommendationEngine.recommendKnot(diaryEntries).id)
            } else {
                KnotType.forDate("$yearMonth-01")
            }
            KnotCreatedDialog(this@MainActivity, knotType) {
                binding.bottomNav.selectedItemId = R.id.nav_knot
            }.show()
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

    override fun onResume() {
        super.onResume()
        // 설정 탭에서 디버그 토글을 바꾸고 돌아온 경우를 반영한다.
        applyDevDateBarVisibility()
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
        updateModeButtonStyles()
        binding.btnModeAutonomous.setOnClickListener { selectMode(MeasurementMode.AUTONOMOUS) }
        binding.btnModeGuide.setOnClickListener { selectMode(MeasurementMode.GUIDE) }

        binding.switchThresholdAlert.isChecked = prefs.isTensionVibrationEnabled
        binding.switchThresholdAlert.setOnCheckedChangeListener { _, checked ->
            prefs.isTensionVibrationEnabled = checked
        }

        binding.groupIdle.setOnClickListener { startMeasurementFlow() }
        binding.btnHelpHome.setOnClickListener {
            HelpImageDialog(this).show()
        }
    }

    private fun startMeasurementFlow() {
        if (prefs.requireBleConnectionToStart && !BleManager.isConnected) {
            Toast.makeText(this, R.string.connection_required_toast, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, MeasurementActivity::class.java).apply {
            // 대기 링의 회전각을 그대로 이어받아 화면이 바뀌어도 링이 끊김없이 이어서 돌게 한다.
            putExtra(MeasurementActivity.EXTRA_IDLE_RING_ANGLE, binding.waveView.currentIdleAngle())
            putExtra(
                MeasurementActivity.EXTRA_MODE,
                if (selectedMode == MeasurementMode.GUIDE) {
                    MeasurementActivity.MODE_GUIDE
                } else {
                    MeasurementActivity.MODE_AUTONOMOUS
                }
            )
        }
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun selectMode(mode: MeasurementMode) {
        if (selectedMode == mode) return
        selectedMode = mode
        updateModeButtonStyles()
    }

    private fun updateModeButtonStyles() {
        val autonomousSelected = selectedMode == MeasurementMode.AUTONOMOUS
        applyModeButtonStyle(binding.btnModeAutonomous, autonomousSelected)
        applyModeButtonStyle(binding.btnModeGuide, !autonomousSelected)
    }

    private fun applyModeButtonStyle(button: TextView, selected: Boolean) {
        button.setBackgroundResource(
            if (selected) R.drawable.bg_mode_button_selected else R.drawable.bg_mode_button_unselected
        )
        button.setTextColor(
            ContextCompat.getColor(this, if (selected) R.color.white else R.color.mode_unselected_text)
        )
    }

    /** 분석 빈 화면의 "측정하러가기"처럼 다른 화면에서 홈(측정 시작) 탭으로 보낼 때 쓴다. */
    fun switchToHomeTab() {
        binding.bottomNav.selectedItemId = R.id.nav_home
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            // 이미 위치한 탭을 다시 눌렀을 땐 재로딩하지 않고 아무 동작도 하지 않는다.
            if (item.itemId == currentTabId) {
                return@setOnItemSelectedListener true
            }
            val direction = tabDirection(currentTabId, item.itemId)
            when (item.itemId) {
                R.id.nav_home -> {
                    switchTab(showHome = true, fragment = null, direction = direction)
                    currentTabId = item.itemId
                    true
                }
                R.id.nav_thread -> {
                    switchTab(showHome = false, fragment = ThreadStorageFragment(), direction = direction)
                    currentTabId = item.itemId
                    true
                }
                R.id.nav_knot -> {
                    switchTab(showHome = false, fragment = KnotStorageFragment(), direction = direction)
                    currentTabId = item.itemId
                    true
                }
                R.id.nav_analysis -> {
                    switchTab(showHome = false, fragment = AnalysisFragment(), direction = direction)
                    currentTabId = item.itemId
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    @Suppress("DEPRECATION")
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    false
                }
                else -> false
            }
        }
    }

    private fun tabDirection(fromId: Int, toId: Int): Int {
        val fromIndex = tabOrder.indexOf(fromId)
        val toIndex = tabOrder.indexOf(toId)
        return if (toIndex >= fromIndex) 1 else -1
    }

    /** 홈 화면(layoutHome)과 프래그먼트 탭(fragmentContainer) 사이를 빠른 슬라이드 애니메이션으로 전환한다. */
    private fun switchTab(showHome: Boolean, fragment: Fragment?, direction: Int) {
        val outgoing = if (binding.layoutHome.visibility == View.VISIBLE) binding.layoutHome else binding.fragmentContainer
        val incoming = if (showHome) binding.layoutHome else binding.fragmentContainer

        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }

        val width = binding.root.width.takeIf { it > 0 }?.toFloat() ?: resources.displayMetrics.widthPixels.toFloat()
        val outTarget = if (direction >= 0) -width else width
        val inStart = if (direction >= 0) width else -width

        if (outgoing === incoming) {
            // 프래그먼트↔프래그먼트 전환: 같은 컨테이너 뷰 안에서 내용만 바뀌므로
            // (outgoing과 incoming이 동일한 뷰) 슬라이드 인 애니메이션만 적용한다.
            // outgoing 쪽 애니메이션까지 같은 뷰에 걸면 animate().cancel()이 서로의
            // 애니메이션을 취소시키면서 visibility=GONE으로 끝나버리는 문제가 있었다.
            incoming.animate().cancel()
            incoming.translationX = inStart
            incoming.visibility = View.VISIBLE
            incoming.animate()
                .translationX(0f)
                .setDuration(TAB_SWITCH_ANIM_MS)
                .start()
            return
        }

        outgoing.animate().cancel()
        outgoing.translationX = 0f
        outgoing.animate()
            .translationX(outTarget)
            .setDuration(TAB_SWITCH_ANIM_MS)
            .withEndAction {
                outgoing.visibility = View.GONE
                outgoing.translationX = 0f
            }
            .start()

        incoming.animate().cancel()
        incoming.translationX = inStart
        incoming.visibility = View.VISIBLE
        incoming.animate()
            .translationX(0f)
            .setDuration(TAB_SWITCH_ANIM_MS)
            .start()
    }

    /** 홈이 아닌 탭에 있을 때 뒤로가기를 누르면 홈 탭으로 돌아간다. */
    override fun onBackPressed() {
        if (binding.layoutHome.visibility != View.VISIBLE) {
            binding.bottomNav.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val EXTRA_NAVIGATE_TO_KNOT = "NAVIGATE_TO_KNOT"
        private const val TAB_SWITCH_ANIM_MS = 150L
        private val DEV_DATE_DISPLAY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd (E)")
        private val DEV_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    }
}
