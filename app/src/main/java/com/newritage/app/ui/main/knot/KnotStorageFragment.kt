package com.newritage.app.ui.main.knot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.databinding.FragmentKnotStorageBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class KnotStorageFragment : Fragment() {

    private var _binding: FragmentKnotStorageBinding? = null
    private val binding get() = _binding!!
    private var currentCalendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKnotStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            // 매듭은 달 단위로 정해지며, 일기(emotion)를 남긴 날들이 그 달의 매듭 추천에 반영된다.
            val dateSet = sessions.filter { it.emotion.isNotBlank() }.map { it.date }.toSet()
            renderCalendar(yearMonth, dateSet)
        }
    }

    private fun renderCalendar(yearMonth: String, dateSet: Set<String>) {
        val grid = binding.calendarGrid
        grid.removeAllViews()
        val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            val dateStr = "$yearMonth-${String.format("%02d", day)}"
            val hasKnot = dateSet.contains(dateStr)

            val cellView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_calendar_cell_knot, grid, false)

            cellView.findViewById<TextView>(R.id.tvDay).text = day.toString()
            cellView.findViewById<View>(R.id.knotIndicator).visibility =
                if (hasKnot) View.VISIBLE else View.INVISIBLE

            if (hasKnot) {
                cellView.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, KnotDetailFragment.newInstance(dateStr))
                        .addToBackStack(null)
                        .commit()
                }
            }
            grid.addView(cellView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
