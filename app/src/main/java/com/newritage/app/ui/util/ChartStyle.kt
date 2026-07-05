package com.newritage.app.ui.util

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis

/** 일/주/월간 분석 화면의 압력 라인차트에 공통으로 적용하는 스타일. */
fun LineChart.applyAnalysisStyle() {
    description.isEnabled = false
    legend.isEnabled = false
    setTouchEnabled(false)
    setGridBackgroundColor(Color.TRANSPARENT)
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.textColor = Color.parseColor("#5A6B5A")
    xAxis.setDrawGridLines(false)
    axisLeft.textColor = Color.parseColor("#5A6B5A")
    axisRight.isEnabled = false
}
