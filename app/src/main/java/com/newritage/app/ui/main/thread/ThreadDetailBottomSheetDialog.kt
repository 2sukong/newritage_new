package com.newritage.app.ui.main.thread

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.ThreadColorManager
import com.newritage.app.databinding.BottomSheetThreadDetailBinding
import com.newritage.app.ui.util.configureFixedHeightSheet
import com.newritage.app.ui.util.setNavArrowEnabled
import com.newritage.app.util.ThreadColors
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
    initialDate: String,
    /**
     * 디버그 "전체 실 목록 보기"에서 넘어온 카탈로그 시작 인덱스([ThreadColors.ALL] 기준).
     * null이 아니면 DB 조회 대신 카탈로그 모드로 동작하며, 좌우 버튼으로 전체 실 색상을 순환한다.
     */
    private val overrideColorIndex: Int? = null
) : BottomSheetDialog(hostActivity) {

    // 카탈로그 모드에서 현재 보고 있는 실 색상 인덱스
    private var catalogIndex: Int = 0

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

        if (overrideColorIndex != null) {
            // 카탈로그 모드: 좌우 버튼으로 전체 실 색상을 순환한다.
            catalogIndex = overrideColorIndex
            binding.btnPrevDay.setOnClickListener { stepCatalog(-1) }
            binding.btnNextDay.setOnClickListener { stepCatalog(+1) }
            renderCatalog()
            return
        }

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

    private fun stepCatalog(delta: Int) {
        val next = catalogIndex + delta
        // 양 끝을 넘어가지 않는다(끝에서는 화살표가 비활성이라 여기 오지 않지만 안전장치).
        if (next < 0 || next >= ThreadColors.ALL.size) return
        catalogIndex = next
        renderCatalog()
    }

    /** 디버그 카탈로그: 실제 세션 없이 [ThreadColors.ALL]의 현재 색상을 보여준다(일기 영역은 숨김). */
    private fun renderCatalog() {
        val color = ThreadColors.ALL[catalogIndex]
        binding.tvDateDisplay.text = color.nameKr
        binding.cvThreadCircle.visibility = View.VISIBLE
        binding.tvThreadColorName.visibility = View.VISIBLE
        binding.tvThreadColorName.text = color.nameKr
        binding.threadColorView.setImageResource(color.drawableRes)
        binding.svDiary.visibility = View.GONE
        binding.tvNoThread.visibility = View.GONE
        // 양 끝에서는 해당 방향 화살표를 회색 비활성 처리한다.
        binding.btnPrevDay.setNavArrowEnabled(catalogIndex > 0)
        binding.btnNextDay.setNavArrowEnabled(catalogIndex < ThreadColors.ALL.size - 1)
    }

    private fun loadData() {
        val dateStr = sdf.format(currentDate.time)
        binding.tvDateDisplay.text = displaySdf.format(currentDate.time)

        hostActivity.lifecycleScope.launch {
            val session = dao.getThreadSessionByDate(dateStr)
            if (session != null && session.threadColorName.isNotEmpty()) {
                binding.cvThreadCircle.visibility = View.VISIBLE
                binding.tvThreadColorName.visibility = View.VISIBLE
                binding.svDiary.visibility = View.VISIBLE
                binding.tvNoThread.visibility = View.GONE
                // master와 동일하게 실 색상 "이름"으로 색상별 사진을 매핑한다(ThreadColorManager).
                binding.threadColorView.setImageResource(
                    ThreadColorManager.getDrawableByColorName(session.threadColorName)
                )
                binding.tvThreadColorName.text = session.threadColorName
                // 그 날 작성한 일기(emotion)를 실 아래 박스에 그대로 표시한다.
                binding.tvDiary.text = session.emotion.ifBlank {
                    hostActivity.getString(com.newritage.app.R.string.thread_diary_empty)
                }
            } else {
                binding.cvThreadCircle.visibility = View.INVISIBLE
                binding.tvThreadColorName.visibility = View.GONE
                binding.svDiary.visibility = View.GONE
                binding.tvNoThread.visibility = View.VISIBLE
            }
        }
    }
}
