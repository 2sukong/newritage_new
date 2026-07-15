package com.newritage.app.ui.main.thread

import android.graphics.Color
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.newritage.app.data.AppDatabase
import com.newritage.app.databinding.BottomSheetThreadDetailBinding
import com.newritage.app.ui.util.configureFixedHeightSheet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 실 보관함 그리드 셀을 탭하면 화면의 90%를 덮는 바텀시트로 올라오는 실 상세 화면.
 * 위로 스와이프다운하거나 상단 "<" 버튼을 누르면 닫힌다.
 */
class ThreadDetailBottomSheetDialog(
    private val hostActivity: FragmentActivity,
    initialDate: String
) : BottomSheetDialog(hostActivity) {

    private lateinit var binding: BottomSheetThreadDetailBinding
    private val dao by lazy { AppDatabase.getInstance(hostActivity).sessionDao() }
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displaySdf = SimpleDateFormat("M월 d일", Locale.getDefault())
    private val currentDate = Calendar.getInstance().apply {
        runCatching { time = sdf.parse(initialDate) ?: Date() }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        binding = BottomSheetThreadDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sheetRoot.topCornerRadiusPx = 28f * hostActivity.resources.displayMetrics.density
        val behavior = configureFixedHeightSheet(
            hostActivity,
            heightFraction = 0.9f,
            cornerRadiusPx = binding.sheetRoot.topCornerRadiusPx
        )

        // dismiss()를 바로 부르면 물리적 슬라이드 없이 즉시 닫혀 배경 페이드아웃과 어긋난다.
        // STATE_HIDDEN으로 바꿔 스와이프다운과 동일한 슬라이드 애니메이션을 타게 한다.
        binding.btnBack.setOnClickListener { behavior.state = BottomSheetBehavior.STATE_HIDDEN }
        binding.btnPrevDay.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            loadData()
        }
        binding.btnNextDay.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, 1)
            loadData()
        }

        loadData()
    }

    private fun loadData() {
        val dateStr = sdf.format(currentDate.time)
        binding.tvDateDisplay.text = displaySdf.format(currentDate.time)

        hostActivity.lifecycleScope.launch {
            val session = dao.getThreadSessionByDate(dateStr)
            if (session != null && session.threadColor.isNotEmpty()) {
                binding.cvThreadCircle.visibility = View.VISIBLE
                binding.tvThreadColorName.visibility = View.VISIBLE
                binding.tvNoThread.visibility = View.GONE
                runCatching {
                    binding.threadColorView.setBackgroundColor(Color.parseColor(session.threadColor))
                }
                binding.tvThreadColorName.text = session.threadColorName
            } else {
                binding.cvThreadCircle.visibility = View.INVISIBLE
                binding.tvThreadColorName.visibility = View.GONE
                binding.tvNoThread.visibility = View.VISIBLE
            }
        }
    }
}
