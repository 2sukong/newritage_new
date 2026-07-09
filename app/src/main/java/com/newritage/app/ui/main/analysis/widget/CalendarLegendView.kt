package com.newritage.app.ui.main.analysis.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.newritage.app.databinding.ViewCalendarLegendBinding

/** 캘린더 색상 범례(안정/보통/긴장/기록없음). 캘린더와 독립된 View로 분리되어 있다. */
class CalendarLegendView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        orientation = HORIZONTAL
        ViewCalendarLegendBinding.inflate(LayoutInflater.from(context), this)
    }
}
