package com.newritage.app.ui.main.analysis.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.newritage.app.R
import com.newritage.app.databinding.ViewComparisonItemBinding
import com.newritage.app.ui.main.analysis.model.TrendDirection

/** 비교 카드 안의 한 항목(Title / Arrow / Value)을 표현하는 독립 View */
class ComparisonItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding: ViewComparisonItemBinding

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        binding = ViewComparisonItemBinding.inflate(LayoutInflater.from(context), this)
    }

    fun setTitle(title: String) {
        binding.tvTitle.text = title
    }

    /**
     * @param increaseIsPositive 증가가 좋은 변화인 항목(명상 시간/횟수)이면 true,
     * 증가가 나쁜 변화인 항목(평균/최고 압력)이면 false
     */
    fun setValue(direction: TrendDirection, valueText: String, increaseIsPositive: Boolean) {
        val colorRes = when (direction) {
            TrendDirection.UP -> if (increaseIsPositive) R.color.trend_green else R.color.trend_red
            TrendDirection.DOWN -> if (increaseIsPositive) R.color.trend_red else R.color.trend_green
            TrendDirection.FLAT, TrendDirection.NONE -> R.color.text_hint
        }
        val arrow = when (direction) {
            TrendDirection.UP -> "▲"
            TrendDirection.DOWN -> "▼"
            TrendDirection.FLAT, TrendDirection.NONE -> "－"
        }
        val color = ContextCompat.getColor(context, colorRes)

        binding.tvArrow.text = arrow
        binding.tvArrow.setTextColor(color)
        binding.tvValue.text = valueText
        binding.tvValue.setTextColor(color)
    }
}
