package com.newritage.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Base64
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.newritage.app.data.SensorReading
import java.io.ByteArrayOutputStream

/**
 * 명상 원시 데이터를 바탕으로 화면에 보이지 않는 차트를 생성하고,
 * 이를 비트맵으로 캡처하여 Base64 문자열로 변환하는 유틸리티.
 */
object ChartCaptureUtil {

    private const val CHART_WIDTH = 800
    private const val CHART_HEIGHT = 500

    /** [readings]를 MPAndroidChart로 렌더링하고 PNG Base64로 반환한다. */
    fun captureChartBase64(context: Context, readings: List<SensorReading>, baseline: Float): String? {
        if (readings.isEmpty()) return null

        val chart = LineChart(context).apply {
            layout(0, 0, CHART_WIDTH, CHART_HEIGHT)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setBackgroundColor(Color.WHITE)
            
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            
            axisLeft.axisMinimum = 0f
            // 전체 최대값보다 약간 여유 있게 설정
            axisLeft.axisMaximum = (readings.maxOfOrNull { it.overall } ?: baseline) * 1.2f
            axisRight.isEnabled = false
        }

        val entries = readings.mapIndexed { index, reading ->
            Entry(index.toFloat(), reading.overall)
        }

        val dataSet = LineDataSet(entries, "Pressure").apply {
            color = Color.parseColor("#8B9E7B")
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            setDrawFilled(true)
            fillColor = Color.parseColor("#8B9E7B")
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.data = LineData(dataSet)
        
        // 차트 뷰를 강제로 그리기 위해 measure/layout 호출
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(CHART_WIDTH, android.view.View.MeasureSpec.EXACTLY)
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(CHART_HEIGHT, android.view.View.MeasureSpec.EXACTLY)
        chart.measure(widthSpec, heightSpec)
        chart.layout(0, 0, CHART_WIDTH, CHART_HEIGHT)

        val bitmap = Bitmap.createBitmap(CHART_WIDTH, CHART_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        chart.draw(canvas)

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        val bytes = outputStream.toByteArray()
        
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
