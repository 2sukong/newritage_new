package com.newritage.app.ui.main.thread

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.databinding.FragmentThreadStorageBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ThreadStorageFragment : Fragment() {

    private var _binding: FragmentThreadStorageBinding? = null
    private val binding get() = _binding!!

    private var currentCalendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThreadStorageBinding.inflate(inflater, container, false)
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
        val displayMonth = SimpleDateFormat("yyyy년 MM월", Locale.getDefault()).format(currentCalendar.time)
        binding.tvMonthLabel.text = displayMonth

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val sessions = db.sessionDao().getSessionsByMonth(yearMonth)
            // 실은 하루 첫 세션(hasThread=true)에만 부여됨
            val colorMap = sessions.filter { it.hasThread }.associate { it.date to it.threadColor }
            renderCalendar(colorMap)
        }
    }

    private fun renderCalendar(colorMap: Map<String, String>) {
        val grid = binding.calendarGrid
        grid.removeAllViews()

        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val yearMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
        val todayStr = sdf.format(Date())

        for (day in 1..daysInMonth) {
            val dateStr = "$yearMonthStr-${String.format("%02d", day)}"
            val threadColor = colorMap[dateStr] ?: continue // 실을 얻지 못한 날은 칸 자체를 만들지 않는다.

            val cellView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_calendar_cell, grid, false)

            val tvDay = cellView.findViewById<TextView>(R.id.tvDay)
            val threadSquareFrame = cellView.findViewById<FrameLayout>(R.id.threadSquareFrame)
            val threadSwatch = cellView.findViewById<View>(R.id.threadSwatch)
            val tvNewBadge = cellView.findViewById<View>(R.id.tvNewBadge)

            tvDay.text = getString(R.string.day_number_format, day)
            val isNew = dateStr == todayStr
            tvNewBadge.visibility = if (isNew) View.VISIBLE else View.GONE
            threadSquareFrame.setBackgroundResource(R.drawable.calender_frame)

            try {
                val cornerRadius = resources.displayMetrics.density * 9f
                threadSwatch.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    this.cornerRadius = cornerRadius
                    setColor(Color.parseColor(threadColor))
                }
                threadSwatch.visibility = View.VISIBLE
            } catch (e: IllegalArgumentException) {
                threadSwatch.visibility = View.INVISIBLE
            }

            grid.addView(cellView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
