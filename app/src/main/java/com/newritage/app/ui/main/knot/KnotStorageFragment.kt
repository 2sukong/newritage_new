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
import com.newritage.app.databinding.FragmentKnotStorageBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KnotStorageFragment : Fragment() {

    private var _binding: FragmentKnotStorageBinding? = null
    private val binding get() = _binding!!
    private var currentCalendar = Calendar.getInstance()
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
                onEntryClick = { dateStr ->
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, KnotDetailFragment.newInstance(dateStr))
                        .addToBackStack(null)
                        .commit()
                }
            )
        }

        binding.btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            loadCalendar()
        }
        binding.btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            loadCalendar()
        }
        loadCalendar()
    }

    private fun loadCalendar() {
        val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.time)
        binding.tvMonthLabel.text =
            SimpleDateFormat("yyyy년 MM월", Locale.getDefault()).format(currentCalendar.time)

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val sessions = db.sessionDao().getSessionsByMonth(yearMonth)
            // 매듭은 하루 첫 세션(hasThread=true)에만 생긴다 — 얻지 못한 날은 아예 칸을 만들지 않는다.
            val knotSessions = sessions.filter { it.hasThread }.associateBy { it.date }
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            entriesState.value = knotSessions.entries
                .sortedBy { it.key }
                .map { (dateStr, session) ->
                    val day = dateStr.substringAfterLast("-").toIntOrNull() ?: 1
                    KnotGridEntry(
                        date = dateStr,
                        day = day,
                        knotType = KnotType.forDate(dateStr),
                        // TODO: '이달의 색상' 산출 알고리즘이 아직 없어, 정해질 때까지는 그날의 실 색을 그대로 임시로 씌운다.
                        tintColorHex = session.threadColor,
                        isNew = dateStr == todayStr
                    )
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
