package com.newritage.app.ui.main.analysis.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.newritage.app.R
import com.newritage.app.databinding.ViewMonthCalendarBinding
import com.newritage.app.ui.main.analysis.model.DayIndicatorStatus
import java.util.Calendar

/**
 * "한 달 한눈에 보기" 캘린더. 날짜마다 작은 원형 Indicator만 표시하는 간결한 달력으로,
 * 요일 헤더와 날짜 그리드를 독립된 View로 구성한다.
 */
class MonthCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding: ViewMonthCalendarBinding

    init {
        orientation = VERTICAL
        binding = ViewMonthCalendarBinding.inflate(LayoutInflater.from(context), this)
    }

    /**
     * @param year 표시할 연도
     * @param month 표시할 월 (1~12)
     * @param indicators 일(day, 1~31)별 상태. 데이터가 없는 날짜는 넣지 않으면 [DayIndicatorStatus.NONE]으로 표시된다.
     */
    fun bind(year: Int, month: Int, indicators: Map<Int, DayIndicatorStatus>) {
        val gridContainer = binding.gridContainer
        gridContainer.removeAllViews()

        val calendar = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1)
        }
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=일요일 ... 7=토요일
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        var row = newRow()
        gridContainer.addView(row)
        repeat(firstDayOfWeek - 1) {
            row.addView(newCell())
        }

        for (day in 1..daysInMonth) {
            if (row.childCount == 7) {
                row = newRow()
                gridContainer.addView(row)
            }
            row.addView(newDayCell(day, indicators[day] ?: DayIndicatorStatus.NONE))
        }
        while (row.childCount < 7) {
            row.addView(newCell())
        }
    }

    private fun newRow(): LinearLayout = LinearLayout(context).apply {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private fun newCell(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(0, dp(28), 1f)
    }

    private fun newDayCell(day: Int, status: DayIndicatorStatus): View {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, dp(4), 0, dp(4))

            addView(TextView(context).apply {
                text = day.toString()
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            })
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                    topMargin = dp(4)
                }
                background = ContextCompat.getDrawable(context, R.drawable.bg_calendar_dot)?.mutate()?.apply {
                    setTint(ContextCompat.getColor(context, statusColorRes(status)))
                }
            })
        }
    }

    private fun statusColorRes(status: DayIndicatorStatus): Int = when (status) {
        DayIndicatorStatus.STABLE -> R.color.trend_green
        DayIndicatorStatus.NORMAL -> R.color.trend_orange
        DayIndicatorStatus.TENSE -> R.color.trend_red
        DayIndicatorStatus.NONE -> R.color.trend_gray
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
