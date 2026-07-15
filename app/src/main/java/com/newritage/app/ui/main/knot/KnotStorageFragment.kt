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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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
            val knotSessions = sessions.filter { it.hasThread }
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // 매듭은 월 단위로 지급되므로, 한 달에 여러 날 매듭을 얻었어도 그 달의 마지막
            // 기록일을 "이달의 매듭" 대표값으로 쓴다(대표값 산출 알고리즘이 정해지기 전까지의 임시 기준).
            val byMonth: Map<Int, Session> = knotSessions
                .groupBy { it.date.substring(5, 7).toInt() }
                .mapValues { (_, list) -> list.maxByOrNull { it.date }!! }

            entriesState.value = byMonth.entries
                .sortedBy { it.key }
                .map { (month, session) ->
                    val yearMonth = String.format(Locale.getDefault(), "%04d-%02d", currentYear, month)
                    KnotGridEntry(
                        key = yearMonth,
                        label = getString(R.string.month_number_format, month),
                        knotType = KnotType.forDate(session.date),
                        tintColorHex = session.threadColor,
                        isNew = session.date == todayStr
                    )
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
