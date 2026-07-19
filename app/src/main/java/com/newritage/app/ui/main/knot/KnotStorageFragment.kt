package com.newritage.app.ui.main.knot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.KnotType
import com.newritage.app.data.Session
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.FragmentKnotStorageBinding
import com.newritage.app.ui.main.knot.recommend.DiaryEntry
import com.newritage.app.ui.main.knot.recommend.RecommendationEngine
import com.newritage.app.util.DevClock
import com.newritage.app.util.ThreadColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

class KnotStorageFragment : Fragment() {

    private var _binding: FragmentKnotStorageBinding? = null
    private val binding get() = _binding!!
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val entriesState = mutableStateOf<List<KnotGridEntry>>(emptyList())
    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKnotStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.knotComposeGrid.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.knotComposeGrid.setContent {
            KnotStorageGrid(
                entries = entriesState.value,
                onEntryClick = { key ->
                    if (prefs.debugShowAllCollection) {
                        // 카탈로그 셀: 대응하는 실제 달이 없으므로 매듭 인덱스를 넘겨 상세를 연다(좌우로 순환 가능).
                        entriesState.value.firstOrNull { it.key == key }?.let { entry ->
                            KnotDetailBottomSheetDialog(
                                requireActivity(),
                                initialYearMonth = "",
                                overrideKnotIndex = entry.knotType.ordinal
                            ).show()
                        }
                    } else {
                        KnotDetailBottomSheetDialog(requireActivity(), key).show()
                    }
                }
            )
        }

        binding.btnPrevYear.setOnClickListener {
            currentYear -= 1
            loadYear()
        }
        binding.btnNextYear.setOnClickListener {
            currentYear += 1
            loadYear()
        }
        loadYear()
    }

    override fun onResume() {
        super.onResume()
        // 설정 탭에서 "전체 실/매듭 목록 보기" 토글을 바꾸고 돌아온 경우를 반영한다.
        loadYear()
    }

    private fun loadYear() {
        // 디버그: 전체 매듭(10종) 카탈로그 표시 모드
        if (prefs.debugShowAllCollection) {
            binding.tvYearLabel.text = getString(R.string.settings_debug_show_all)
            // 매듭마다 서로 다른 실 색을 임의로 입혀 구분이 잘 되게 한다(순서대로 12색 순환).
            entriesState.value = KnotType.entries.mapIndexed { index, knotType ->
                KnotGridEntry(
                    key = knotType.name,
                    label = knotType.displayName,
                    knotType = knotType,
                    tintColorHex = ThreadColors.ALL[index % ThreadColors.ALL.size].hex,
                    isNew = false
                )
            }
            return
        }

        binding.tvYearLabel.text = getString(R.string.year_number_format, currentYear)

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val sessions = db.sessionDao().getSessionsByYear(currentYear.toString())
            // NEW 배지는 시연용 가상 오늘 기준으로 표시한다.
            val todayStr = DevClock.todayString(requireContext())

            fun monthOf(session: Session) = session.date.substring(5, 7).toInt()

            // 매듭은 월 단위로 지급되며, 그 달에 쓴 일기(emotion)들을 감정 분석해 "이달의 매듭"을
            // 추천한다(일기가 하나도 없는 달은 칸 자체가 생기지 않는다).
            val emotionByMonth: Map<Int, List<Session>> = sessions
                .filter { it.emotion.isNotBlank() }
                .groupBy(::monthOf)

            // 틴트 색상은 sumin_new처럼 그 달에 실을 받은(threadColor가 있는) 세션에서 가져온다.
            // 일기 유무와 무관하게 그 달 전체 세션을 보므로, 일기를 건너뛴 날에 받은 실 색도 반영된다.
            val threadColorByMonth: Map<Int, String> = sessions
                .filter { it.threadColor.isNotBlank() }
                .groupBy(::monthOf)
                .mapValues { (_, list) -> list.maxByOrNull { it.date }!!.threadColor }

            entriesState.value = emotionByMonth.entries
                .sortedBy { it.key }
                .map { (month, monthSessions) ->
                    val yearMonth = String.format(Locale.getDefault(), "%04d-%02d", currentYear, month)
                    val diaryEntries = monthSessions.map {
                        DiaryEntry(date = LocalDate.parse(it.date), content = it.emotion)
                    }
                    val recommendedKnot = RecommendationEngine.recommendKnot(diaryEntries)
                    val latestSession = monthSessions.maxByOrNull { it.date }!!
                    KnotGridEntry(
                        key = yearMonth,
                        label = getString(R.string.month_number_format, month),
                        knotType = KnotType.fromRecommendationId(recommendedKnot.id),
                        tintColorHex = threadColorByMonth[month] ?: "",
                        isNew = latestSession.date == todayStr
                    )
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
