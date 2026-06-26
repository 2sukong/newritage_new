package com.newritage.app.ui.util

import android.content.Context
import android.util.AttributeSet
import android.widget.GridLayout

class CalendarGridLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr) {
    init { columnCount = 4 }
}
