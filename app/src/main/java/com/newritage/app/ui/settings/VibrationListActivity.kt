package com.newritage.app.ui.settings

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.newritage.app.R
import com.newritage.app.ble.BleManager
import com.newritage.app.ble.VibrationPattern
import com.newritage.app.ble.VibrationPatterns
import com.newritage.app.ble.VibrationType
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityVibrationListBinding

class VibrationListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TYPE = "extra_type"
        private val TIMER_DURATION_OPTIONS = listOf(5, 10, 15, 20)
        private const val PREVIEW_BASE_DURATION_MS = 600L
    }

    private lateinit var binding: ActivityVibrationListBinding
    private lateinit var prefs: UserPreferences
    private lateinit var adapter: VibrationAdapter
    private lateinit var type: VibrationType

    private var pendingTimerDuration = 0
    private val timeChipViews = mutableListOf<TextView>()

    private val previewHandler = Handler(Looper.getMainLooper())
    private var previewRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVibrationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        type = VibrationType.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: VibrationType.TIMER.name)
        prefs = UserPreferences(this)

        setupUI()
        connectBle()
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) {
                BleManager.startScan(this)
            }
        }

    private fun connectBle() {
        if (BleManager.hasRequiredPermissions(this)) {
            BleManager.startScan(this)
        } else {
            permissionLauncher.launch(BleManager.requiredPermissions())
        }
    }

    private fun setupUI() {
        val isTimer = type == VibrationType.TIMER

        binding.btnBack.setOnClickListener { finish() }

        binding.tvTitle.text = getString(
            if (isTimer) R.string.vibration_timer_title else R.string.vibration_tension_title
        )
        binding.tvDesc.text = getString(
            if (isTimer) R.string.vibration_timer_select_desc else R.string.vibration_tension_select_desc
        )

        val defaultId = if (isTimer) VibrationPatterns.TIMER_DEFAULT_ID else VibrationPatterns.TENSION_DEFAULT_ID
        val items = VibrationPatterns.ALL.filter { it.type == type }

        adapter = VibrationAdapter(
            items = items,
            onPlayClick = { togglePreview(it) },
            onSelectClick = { selectPattern(it) }
        )
        binding.recyclerVibration.layoutManager = LinearLayoutManager(this)
        binding.recyclerVibration.adapter = adapter

        adapter.enabled = if (isTimer) prefs.isTimerVibrationEnabled else prefs.isTensionVibrationEnabled
        adapter.selectedId = (if (isTimer) prefs.timerVibrationPatternId else prefs.tensionVibrationPatternId)
            ?: defaultId

        binding.switchEnable.isChecked = adapter.enabled
        binding.switchEnable.setOnCheckedChangeListener { _, checked ->
            adapter.enabled = checked
            if (isTimer) prefs.isTimerVibrationEnabled = checked else prefs.isTensionVibrationEnabled = checked
            if (!checked) stopPreview()
            updateTimeChipsEnabled()
            updateSelectButton()
        }

        if (isTimer) {
            pendingTimerDuration = prefs.timerDurationMinutes
            setupTimeChips()
        } else {
            binding.layoutTimeChips.visibility = View.GONE
        }

        updateSelectButton()

        binding.btnSelectComplete.setOnClickListener {
            val selected = adapter.selectedId
            if (binding.switchEnable.isChecked && selected != null) {
                if (isTimer) {
                    prefs.timerVibrationPatternId = selected
                    prefs.timerDurationMinutes = pendingTimerDuration
                } else {
                    prefs.tensionVibrationPatternId = selected
                }
                finish()
            }
        }
    }

    private fun setupTimeChips() {
        binding.layoutTimeChips.visibility = View.VISIBLE
        binding.layoutTimeChips.removeAllViews()
        timeChipViews.clear()

        val marginPx = (6 * resources.displayMetrics.density).toInt()
        TIMER_DURATION_OPTIONS.forEach { minutes ->
            val chip = TextView(this).apply {
                text = getString(R.string.vibration_timer_duration_format, minutes)
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, marginPx * 3, 0, marginPx * 3)
                background = ContextCompat.getDrawable(context, R.drawable.bg_time_chip)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    weight = 1f
                    marginStart = marginPx
                    marginEnd = marginPx
                }
                setOnClickListener {
                    pendingTimerDuration = minutes
                    updateTimeChipsSelection()
                }
            }
            timeChipViews.add(chip)
            binding.layoutTimeChips.addView(chip)
        }
        updateTimeChipsSelection()
        updateTimeChipsEnabled()
    }

    private fun updateTimeChipsSelection() {
        timeChipViews.zip(TIMER_DURATION_OPTIONS).forEach { (chip, minutes) ->
            val isSelected = minutes == pendingTimerDuration
            chip.backgroundTintList = if (isSelected) {
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))
            } else {
                null
            }
            chip.setTextColor(
                ContextCompat.getColor(this, if (isSelected) R.color.white else R.color.text_primary)
            )
        }
    }

    private fun updateTimeChipsEnabled() {
        val enabled = binding.switchEnable.isChecked
        timeChipViews.forEach {
            it.isEnabled = enabled
            it.alpha = if (enabled) 1f else 0.4f
        }
    }

    private fun togglePreview(pattern: VibrationPattern) {
        if (adapter.playingId == pattern.id) {
            stopPreview()
            return
        }
        previewRunnable?.let { previewHandler.removeCallbacks(it) }

        BleManager.sendVibration(pattern.effect, pattern.count, pattern.intervalMs)
        adapter.playingId = pattern.id

        val previewDurationMs = if (pattern.count > 1) {
            (pattern.count - 1) * pattern.intervalMs.toLong() + PREVIEW_BASE_DURATION_MS
        } else {
            PREVIEW_BASE_DURATION_MS
        }
        val runnable = Runnable { adapter.playingId = null }
        previewRunnable = runnable
        previewHandler.postDelayed(runnable, previewDurationMs)
    }

    private fun stopPreview() {
        BleManager.sendStop()
        previewRunnable?.let { previewHandler.removeCallbacks(it) }
        adapter.playingId = null
    }

    private fun selectPattern(pattern: VibrationPattern) {
        adapter.selectedId = pattern.id
        updateSelectButton()
    }

    private fun updateSelectButton() {
        val canConfirm = binding.switchEnable.isChecked && adapter.selectedId != null
        binding.btnSelectComplete.isEnabled = canConfirm
        binding.btnSelectComplete.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, if (canConfirm) R.color.primary else R.color.btn_disabled)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPreview()
    }
}
