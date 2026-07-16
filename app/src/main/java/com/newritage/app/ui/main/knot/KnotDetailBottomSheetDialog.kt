package com.newritage.app.ui.main.knot

import android.os.Bundle
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.KnotType
import com.newritage.app.databinding.BottomSheetKnotDetailBinding
import com.newritage.app.ui.util.configureFixedHeightSheet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 매듭 보관함 연도 그리드의 한 달 칸을 탭하면 화면의 90%를 덮는 바텀시트로 올라오는
 * "이달의 매듭" 상세 화면. 위로 스와이프다운하거나 상단 "<" 버튼을 누르면 닫힌다.
 */
class KnotDetailBottomSheetDialog(
    private val hostActivity: FragmentActivity,
    initialYearMonth: String
) : BottomSheetDialog(hostActivity) {

    private lateinit var binding: BottomSheetKnotDetailBinding
    private val dao by lazy { AppDatabase.getInstance(hostActivity).sessionDao() }
    private val yearMonthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val displaySdf = SimpleDateFormat("yyyy년 M월", Locale.getDefault())
    private val currentDate = Calendar.getInstance().apply {
        runCatching { time = yearMonthSdf.parse(initialYearMonth) ?: Date() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = BottomSheetKnotDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BottomSheetDialog는 호스트 액티비티와 별개의 Window라 lifecycle/ViewModelStore/
        // SavedStateRegistry가 자동으로 전파되지 않는다. ComposeView(knotComposeViewer)가
        // 내부적으로 ViewTreeLifecycleOwner를 요구하므로 decorView에 직접 연결해준다.
        window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(hostActivity)
            decorView.setViewTreeViewModelStoreOwner(hostActivity)
            decorView.setViewTreeSavedStateRegistryOwner(hostActivity)
        }

        binding.sheetRoot.topCornerRadiusPx = 28f * hostActivity.resources.displayMetrics.density
        val behavior = configureFixedHeightSheet(
            hostActivity,
            heightFraction = 0.9f,
            cornerRadiusPx = binding.sheetRoot.topCornerRadiusPx
        )

        binding.knotComposeViewer.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        // dismiss()를 바로 부르면 물리적 슬라이드 없이 즉시 닫혀 배경 페이드아웃과 어긋난다.
        // STATE_HIDDEN으로 바꿔 스와이프다운과 동일한 슬라이드 애니메이션을 타게 한다.
        binding.btnBack.setOnClickListener { behavior.state = BottomSheetBehavior.STATE_HIDDEN }
        binding.btnPrevMonth.setOnClickListener {
            currentDate.add(Calendar.MONTH, -1)
            loadKnotData()
        }
        binding.btnNextMonth.setOnClickListener {
            currentDate.add(Calendar.MONTH, 1)
            loadKnotData()
        }

        loadKnotData()
    }

    private fun loadKnotData() {
        val yearMonth = yearMonthSdf.format(currentDate.time)
        binding.tvDateDisplay.text = displaySdf.format(currentDate.time)

        hostActivity.lifecycleScope.launch {
            // 매듭은 월 단위로 지급되므로, 그 달의 마지막 기록일을 "이달의 매듭" 대표값으로 쓴다
            // (KnotStorageFragment의 연도 그리드와 동일한 임시 기준).
            val session = dao.getSessionsByMonth(yearMonth)
                .filter { it.hasThread }
                .maxByOrNull { it.date }

            if (session != null) {
                val knotType = KnotType.forDate(session.date)
                binding.tvKnotNameDisplay.text =
                    "${session.threadColorName.ifEmpty { "매듭" }} · ${knotType.displayName}"
                // 상세보기: 위치 이동(pan)은 막고 회전(orbit)만 가능하게 한다.
                binding.knotComposeViewer.setContent {
                    KnotModelViewer(
                        glbAssetPath = knotType.assetPath,
                        interactive = true,
                        tintColor = knotTintColorOrNull(session.threadColor),
                        cameraDistance = 1.5f
                    )
                }
            } else {
                binding.tvKnotNameDisplay.text = "기록된 매듭이 없습니다"
                binding.knotComposeViewer.setContent {}
            }
        }
    }
}
