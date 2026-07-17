package com.newritage.app.ui.main.analysis.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.newritage.app.R
import com.newritage.app.databinding.ViewAiCommentCardBinding

/** AI 종합 코멘트를 보여주는 카드. 텍스트 영역([setComment])이 독립적으로 바인딩 가능하다. */
class AiCommentCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding: ViewAiCommentCardBinding

    init {
        orientation = VERTICAL
        setBackgroundResource(R.drawable.acard)
        val pad = dp(20)
        setPadding(pad, pad, pad, pad)
        binding = ViewAiCommentCardBinding.inflate(LayoutInflater.from(context), this)
    }

    fun setComment(comment: String) {
        binding.tvComment.text = comment
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
