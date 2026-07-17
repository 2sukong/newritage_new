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
import com.newritage.app.databinding.FragmentKnotStorageBinding
import com.newritage.app.ui.main.knot.recommend.DiaryEntry
import com.newritage.app.ui.main.knot.recommend.RecommendationEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KnotStorageFragment : Fragment() {

    private var _binding: FragmentKnotStorageBinding? = null
    private val binding get() = _binding!!
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val entriesState = mutableStateOf<List<KnotGridEntry>>(emptyList())

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
                onEntryClick = { yearMonth ->
                    KnotDetailBottomSheetDialog(requireActivity(), yearMonth).show()
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

    private fun loadYear() {
        binding.tvYearLabel.text = getString(R.string.year_number_format, currentYear)

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val sessions = db.sessionDao().getSessionsByYear(currentYear.toString())
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // 매듭은 월 단위로 지급되며, 그 달에 쓴 일기(emotion)들을 감정 분석해 "이달의 매듭"을
            // 추천한다(일기가 하나도 없는 달은 칸 자체가 생기지 않는다).
            val byMonth: Map<Int, List<Session>> = sessions
                .filter { it.emotion.isNotBlank() }
                .groupBy { it.date.substring(5, 7).toInt() }

            entriesState.value = byMonth.entries
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
                        tintColorHex = latestSession.threadColor,
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
