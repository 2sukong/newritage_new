package com.newritage.app.ui.main.thread

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.FragmentThreadStorageBinding
import com.newritage.app.ui.util.GradientBorderDrawable
import com.newritage.app.util.DevClock
import com.newritage.app.util.ThreadColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ThreadStorageFragment : Fragment() {

    private var _binding: FragmentThreadStorageBinding? = null
    private val binding get() = _binding!!

    private var currentCalendar = Calendar.getInstance()
    private val prefs by lazy { UserPreferences(requireContext()) }

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

    override fun onResume() {
        super.onResume()
        // 설정 탭에서 "전체 실/매듭 목록 보기" 토글을 바꾸고 돌아온 경우를 반영한다.
        loadCalendar()
    }

    private fun loadCalendar() {
        // 디버그: 전체 실 색상(12종) 카탈로그 표시 모드
        if (prefs.debugShowAllCollection) {
            binding.tvMonthLabel.text = getString(R.string.settings_debug_show_all)
            renderAllThreadColors()
            return
        }

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
        // NEW 배지는 시연용 가상 오늘 기준으로 표시한다.
        val todayStr = DevClock.todayString(requireContext())

        // 매듭보관함(Compose 4열 그리드)과 동일하게, 실을 얻은 날짜만 순서대로 4칸씩 줄을 채운다.
        var currentRow: LinearLayout? = null
        var columnInRow = 0

        for (day in 1..daysInMonth) {
            val dateStr = "$yearMonthStr-${String.format("%02d", day)}"
            val threadColor = colorMap[dateStr] ?: continue // 실을 얻지 못한 날은 칸 자체를 만들지 않는다.

            if (columnInRow == 0) {
                currentRow = newGridRow()
                grid.addView(currentRow)
            }

            val cellView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_calendar_cell, currentRow, false)

            val tvDay = cellView.findViewById<TextView>(R.id.tvDay)
            val threadNewFrame = cellView.findViewById<FrameLayout>(R.id.threadNewFrame)
            val threadSquareFrame = cellView.findViewById<FrameLayout>(R.id.threadSquareFrame)
            val threadSwatch = cellView.findViewById<View>(R.id.threadSwatch)
            val tvNewBadge = cellView.findViewById<View>(R.id.tvNewBadge)

            tvDay.text = getString(R.string.day_number_format, day)
            val isNew = dateStr == todayStr
            // GONE 대신 INVISIBLE: NEW 배지 유무와 상관없이 모든 칸이 같은 세로 구조를
            // 가져야 threadSquareFrame이 줄마다 같은 높이에서 정렬된다.
            tvNewBadge.visibility = if (isNew) View.VISIBLE else View.INVISIBLE
            // 회색 그라데이션 테두리는 NEW 여부와 상관없이 항상 그대로 두고, NEW일 때는
            // 그 겉을 감싸는 threadNewFrame에만 별도의 두꺼운 프레임을 씌운다. threadNewFrame은
            // 항상 같은 크기(74x86)라 이 배경 토글이 칸의 레이아웃을 바꾸지 않는다.
            threadNewFrame.background = if (isNew) {
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_rounded_new_outer_border)
            } else {
                null
            }
            threadSquareFrame.background = GradientBorderDrawable(
                strokeWidthPx = resources.displayMetrics.density * 1f,
                cornerRadiusPx = resources.displayMetrics.density * 8f,
                topColor = Color.parseColor("#D6D6D6"),
                bottomColor = Color.parseColor("#BABCBA")
            )

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

            cellView.setOnClickListener {
                ThreadDetailBottomSheetDialog(requireActivity(), dateStr).show()
            }

            currentRow?.addView(cellView)
            columnInRow = (columnInRow + 1) % COLUMN_COUNT
        }

        // 마지막 줄이 4칸을 다 채우지 못했다면, 남은 자리를 투명한 빈 칸으로 채워
        // weight 기반 열 너비가 앞선 줄들과 같은 위치에서 정렬되게 한다.
        if (columnInRow != 0) {
            repeat(COLUMN_COUNT - columnInRow) {
                currentRow?.addView(
                    View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                    }
                )
            }
        }
    }

    /**
     * 디버그 표시 전용: DB와 무관하게 전체 실 색상(ThreadColors.ALL, 12종)을 4칸씩 채워 보여준다.
     * DB를 건드리지 않으므로 토글을 끄면 실제로 모은 실만 다시 보인다. 클릭은 비활성화한다.
     */
    private fun renderAllThreadColors() {
        val grid = binding.calendarGrid
        grid.removeAllViews()

        var currentRow: LinearLayout? = null
        var columnInRow = 0

        ThreadColors.ALL.forEachIndexed { index, color ->
            if (columnInRow == 0) {
                currentRow = newGridRow()
                grid.addView(currentRow)
            }

            val cellView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_calendar_cell, currentRow, false)

            val tvDay = cellView.findViewById<TextView>(R.id.tvDay)
            val threadNewFrame = cellView.findViewById<FrameLayout>(R.id.threadNewFrame)
            val threadSquareFrame = cellView.findViewById<FrameLayout>(R.id.threadSquareFrame)
            val threadSwatch = cellView.findViewById<View>(R.id.threadSwatch)
            val tvNewBadge = cellView.findViewById<View>(R.id.tvNewBadge)

            tvDay.text = getString(R.string.day_number_format, index + 1)
            tvNewBadge.visibility = View.INVISIBLE
            threadNewFrame.background = null
            threadSquareFrame.background = GradientBorderDrawable(
                strokeWidthPx = resources.displayMetrics.density * 1f,
                cornerRadiusPx = resources.displayMetrics.density * 8f,
                topColor = Color.parseColor("#D6D6D6"),
                bottomColor = Color.parseColor("#BABCBA")
            )

            try {
                val cornerRadius = resources.displayMetrics.density * 9f
                threadSwatch.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    this.cornerRadius = cornerRadius
                    setColor(Color.parseColor(color.hex))
                }
                threadSwatch.visibility = View.VISIBLE
            } catch (e: IllegalArgumentException) {
                threadSwatch.visibility = View.INVISIBLE
            }

            // 카탈로그 셀은 대응하는 실제 세션이 없으므로 인덱스를 넘겨 상세를 연다(좌우로 순환 가능).
            cellView.setOnClickListener {
                ThreadDetailBottomSheetDialog(
                    requireActivity(), initialDate = "", overrideColorIndex = index
                ).show()
            }

            currentRow?.addView(cellView)
            columnInRow = (columnInRow + 1) % COLUMN_COUNT
        }

        if (columnInRow != 0) {
            repeat(COLUMN_COUNT - columnInRow) {
                currentRow?.addView(
                    View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                    }
                )
            }
        }
    }

    private fun newGridRow(): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val COLUMN_COUNT = 4
    }
}
