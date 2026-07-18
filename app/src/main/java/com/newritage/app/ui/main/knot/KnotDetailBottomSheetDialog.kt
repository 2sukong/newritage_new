package com.newritage.app.ui.main.knot

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.GeminiRepository
import com.newritage.app.data.KnotType
import com.newritage.app.databinding.BottomSheetKnotDetailBinding
import com.newritage.app.ui.main.knot.recommend.DiaryEntry
import com.newritage.app.ui.main.knot.recommend.RecommendationEngine
import com.newritage.app.ui.util.configureFixedHeightSheet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
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
    private val geminiRepository by lazy { GeminiRepository(dao) }
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
            // 그 달에 쓴 일기(emotion)들을 감정 분석해 "이달의 매듭"을 추천한다
            // (KnotStorageFragment의 연도 그리드와 동일한 기준).
            val allMonthSessions = dao.getSessionsByMonth(yearMonth)
            val monthSessions = allMonthSessions.filter { it.emotion.isNotBlank() }
            val latestSession = monthSessions.maxByOrNull { it.date }

            if (latestSession != null) {
                val diaryEntries = monthSessions.map {
                    DiaryEntry(date = LocalDate.parse(it.date), content = it.emotion)
                }
                // 최근 30일 감정 기록으로 Gemini API를 우선 시도하고, 실패 시에만 표시 중인 달 기준
                // 로컬 추천(RecommendationEngine)으로 폴백한다.
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val aiResult = geminiRepository.recommendKnot(todayStr)
                val recommendedKnot = aiResult?.knot ?: RecommendationEngine.recommendKnot(diaryEntries)
                val knotType = KnotType.fromRecommendationId(recommendedKnot.id)
                binding.tvKnotNameDisplay.text = recommendedKnot.name
                binding.tvDescriptionText.text = recommendedKnot.meaning

                if (aiResult != null) {
                    binding.tvKnotReason.text = hostActivity.getString(
                        R.string.knot_reason_label
                    ) + "\n\n" + aiResult.reason
                    binding.tvKnotReason.visibility = View.VISIBLE
                } else {
                    binding.tvKnotReason.visibility = View.GONE
                }
                // 틴트 색상은 sumin_new처럼 그 달에 실을 받은(threadColor가 있는) 세션에서 가져온다.
                // 일기 유무와 무관하게 그 달 전체 세션을 보므로, 일기를 건너뛴 날에 받은 실 색도 반영된다.
                val threadColorSession = allMonthSessions.filter { it.threadColor.isNotBlank() }.maxByOrNull { it.date }
                // 상세보기: 위치 이동(pan)은 막고 회전(orbit)만 가능하게 한다.
                binding.knotComposeViewer.setContent {
                    KnotModelViewer(
                        glbAssetPath = knotType.assetPath,
                        interactive = true,
                        tintColor = threadColorSession?.let { knotTintColorOrNull(it.threadColor) },
                        cameraDistance = 1.5f
                    )
                }
            } else {
                binding.tvKnotNameDisplay.text = hostActivity.getString(R.string.no_knot_yet)
                binding.tvDescriptionText.text = ""
                binding.tvKnotReason.visibility = View.GONE
                binding.knotComposeViewer.setContent {}
            }
        }
    }
}
