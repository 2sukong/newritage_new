package com.newritage.app.ui.baseline

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.newritage.app.R
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityBaselineMeasurementBinding
import com.newritage.app.ui.main.MainActivity
import kotlin.random.Random

class BaselineMeasurementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBaselineMeasurementBinding
    private lateinit var prefs: UserPreferences

    private val handler = Handler(Looper.getMainLooper())
    private var measuring = false
    private var elapsedSeconds = 0
    private val measureDuration = 30 // 30초 측정
    private val pressureReadings = mutableListOf<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaselineMeasurementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)

        setupUI()
    }

    private fun setupUI() {
        // 초기: 안내 화면
        showScreen(Screen.GUIDE)

        binding.btnStartMeasure.setOnClickListener {
            startMeasurement()
        }

        binding.btnGoHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun startMeasurement() {
        showScreen(Screen.MEASURING)
        measuring = true
        elapsedSeconds = 0
        pressureReadings.clear()

        binding.progressBar.max = measureDuration
        binding.progressBar.progress = 0

        measureLoop.run()
    }

    private val measureLoop = object : Runnable {
        override fun run() {
            if (!measuring) return

            elapsedSeconds++
            // 시뮬레이션: 편안한 상태 기준 압력 (낮은 편)
            val pressure = 20f + Random.nextFloat() * 15f
            pressureReadings.add(pressure)

            val elapsed = elapsedSeconds
            binding.tvTimer.text = String.format("%d:%02d", elapsed / 60, elapsed % 60)
            binding.progressBar.progress = elapsed

            // 측정값 표시 (평균)
            val avg = pressureReadings.average().toFloat()
            binding.tvPressureValue.text = String.format("%.1f", avg)

            if (elapsedSeconds >= measureDuration) {
                completeMeasurement()
            } else {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    private fun completeMeasurement() {
        measuring = false
        handler.removeCallbacks(measureLoop)

        val baselinePressure = pressureReadings.average().toFloat()
        prefs.baselinePressure = baselinePressure
        prefs.isBaselineDone = true

        binding.tvBaselineResult.text = String.format("%.1f kPa", baselinePressure)
        showScreen(Screen.COMPLETE)
    }

    private fun showScreen(screen: Screen) {
        binding.layoutGuide.visibility = if (screen == Screen.GUIDE) View.VISIBLE else View.GONE
        binding.layoutMeasuring.visibility = if (screen == Screen.MEASURING) View.VISIBLE else View.GONE
        binding.layoutComplete.visibility = if (screen == Screen.COMPLETE) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        measuring = false
        handler.removeCallbacks(measureLoop)
    }

    enum class Screen { GUIDE, MEASURING, COMPLETE }
}
