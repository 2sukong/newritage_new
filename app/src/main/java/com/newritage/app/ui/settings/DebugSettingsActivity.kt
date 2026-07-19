package com.newritage.app.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.newritage.app.R
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityDebugSettingsBinding

/**
 * 설정 탭 맨 아래 "디버그" 항목에서 진입하는 개발/시연용 옵션 화면.
 * 토글 3종을 [UserPreferences]에 저장하며, 각 토글의 실제 동작은 이를 읽는 곳
 * (MainActivity 날짜바/측정 시작, 실·매듭 보관함 표시)에서 처리한다.
 */
class DebugSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebugSettingsBinding
    private lateinit var prefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)

        binding.btnBack.setOnClickListener { finishWithSlideBack() }

        binding.switchDebugDateBar.isChecked = prefs.debugDateBarVisible
        binding.switchDebugDateBar.setOnCheckedChangeListener { _, checked ->
            prefs.debugDateBarVisible = checked
        }

        binding.switchDebugShowAll.isChecked = prefs.debugShowAllCollection
        binding.switchDebugShowAll.setOnCheckedChangeListener { _, checked ->
            prefs.debugShowAllCollection = checked
        }

        binding.switchDebugRequireBle.isChecked = prefs.requireBleConnectionToStart
        binding.switchDebugRequireBle.setOnCheckedChangeListener { _, checked ->
            prefs.requireBleConnectionToStart = checked
        }
    }

    private fun finishWithSlideBack() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
