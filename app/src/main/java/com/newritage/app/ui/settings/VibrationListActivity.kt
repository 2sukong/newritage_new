package com.newritage.app.ui.settings

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.newritage.app.R
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityVibrationListBinding

class VibrationListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TYPE = "extra_type"
    }

    private lateinit var binding: ActivityVibrationListBinding
    private lateinit var prefs: UserPreferences
    private lateinit var vibrator: Vibrator
    private lateinit var adapter: VibrationAdapter
    private lateinit var type: VibrationType

    private val previewHandler = Handler(Looper.getMainLooper())
    private var previewRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVibrationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        type = VibrationType.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: VibrationType.TIMER.name)
        prefs = UserPreferences(this)
        vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)!!

        setupUI()
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

        adapter = VibrationAdapter(
            items = VibrationPatterns.ALL,
            onPlayClick = { togglePreview(it) },
            onSelectClick = { selectPattern(it) }
        )
        binding.recyclerVibration.layoutManager = LinearLayoutManager(this)
        binding.recyclerVibration.adapter = adapter

        adapter.enabled = if (isTimer) prefs.isTimerVibrationEnabled else prefs.isTensionVibrationEnabled
        adapter.selectedId = if (isTimer) prefs.timerVibrationPatternId else prefs.tensionVibrationPatternId

        binding.switchEnable.isChecked = adapter.enabled
        binding.switchEnable.setOnCheckedChangeListener { _, checked ->
            adapter.enabled = checked
            if (isTimer) prefs.isTimerVibrationEnabled = checked else prefs.isTensionVibrationEnabled = checked
            if (!checked) stopPreview()
            updateSelectButton()
        }

        updateSelectButton()

        binding.btnSelectComplete.setOnClickListener {
            val selected = adapter.selectedId
            if (binding.switchEnable.isChecked && selected != null) {
                if (isTimer) prefs.timerVibrationPatternId = selected else prefs.tensionVibrationPatternId = selected
                finish()
            }
        }
    }

    private fun togglePreview(pattern: VibrationPattern) {
        if (adapter.playingId == pattern.id) {
            stopPreview()
            return
        }
        vibrator.cancel()
        vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings, -1))
        adapter.playingId = pattern.id

        previewRunnable?.let { previewHandler.removeCallbacks(it) }
        val runnable = Runnable { adapter.playingId = null }
        previewRunnable = runnable
        previewHandler.postDelayed(runnable, pattern.timings.sum())
    }

    private fun stopPreview() {
        vibrator.cancel()
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
