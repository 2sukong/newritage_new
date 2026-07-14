package com.newritage.app.ui.main.analysis.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.newritage.app.R
import com.newritage.app.databinding.ViewComparisonCardBinding
import com.newritage.app.ui.main.analysis.model.ComparisonSummary
import com.newritage.app.ui.main.analysis.model.TrendDirection
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * "저번주/저번달과 비교" 카드. 평균 압력 / 최고 압력 / 명상 시간 / 명상 횟수를
 * 4등분 레이아웃으로 보여주며, 각 항목은 [ComparisonItemView]로 분리되어 있다.
 * 추후 데이터 바인딩은 [bind]에 [ComparisonSummary]만 넘기면 된다.
 */
class ComparisonCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding: ViewComparisonCardBinding

    init {
        orientation = HORIZONTAL
        isBaselineAligned = false
        setBackgroundResource(R.drawable.acard)
        val vPad = dp(20)
        val hPad = dp(4)
        setPadding(hPad, vPad, hPad, vPad)

        binding = ViewComparisonCardBinding.inflate(android.view.LayoutInflater.from(context), this)
        binding.itemAvgPressure.setTitle("평균 압력")
        binding.itemMaxPressure.setTitle("최고 압력")
        binding.itemMedTime.setTitle("명상 시간")
        binding.itemMedCount.setTitle("명상 횟수")
    }

    fun bind(summary: ComparisonSummary) {
        bindPercent(binding.itemAvgPressure, summary.avgPressureChangePercent, increaseIsPositive = false)
        bindPercent(binding.itemMaxPressure, summary.maxPressureChangePercent, increaseIsPositive = false)
        bindMinutes(binding.itemMedTime, summary.meditationTimeChangeSeconds)
        bindCount(binding.itemMedCount, summary.meditationCountChange)
    }

    private fun bindPercent(item: ComparisonItemView, percent: Float?, increaseIsPositive: Boolean) {
        if (percent == null) {
            item.setValue(TrendDirection.NONE, "-", increaseIsPositive)
            return
        }
        val direction = when {
            percent > 0.05f -> TrendDirection.UP
            percent < -0.05f -> TrendDirection.DOWN
            else -> TrendDirection.FLAT
        }
        val text = String.format(Locale.getDefault(), "%.1f%%", abs(percent))
        item.setValue(direction, text, increaseIsPositive)
    }

    private fun bindMinutes(item: ComparisonItemView, changeSeconds: Int?) {
        if (changeSeconds == null) {
            item.setValue(TrendDirection.NONE, "-", increaseIsPositive = true)
            return
        }
        val changeMinutes = (changeSeconds / 60f).roundToInt()
        val direction = when {
            changeMinutes > 0 -> TrendDirection.UP
            changeMinutes < 0 -> TrendDirection.DOWN
            else -> TrendDirection.FLAT
        }
        val sign = if (changeMinutes >= 0) "+" else ""
        item.setValue(direction, "$sign${changeMinutes}분", increaseIsPositive = true)
    }

    private fun bindCount(item: ComparisonItemView, change: Int?) {
        if (change == null) {
            item.setValue(TrendDirection.NONE, "-", increaseIsPositive = true)
            return
        }
        val direction = when {
            change > 0 -> TrendDirection.UP
            change < 0 -> TrendDirection.DOWN
            else -> TrendDirection.FLAT
        }
        val sign = if (change >= 0) "+" else ""
        item.setValue(direction, "$sign${change}회", increaseIsPositive = true)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
