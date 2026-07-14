package com.newritage.app.ui.main.thread

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.ThreadColorManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ThreadDetailFragment : Fragment() {

    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedDate = it.getString(ARG_DATE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_thread_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        val tvDateDisplay = view.findViewById<TextView>(R.id.tvDateDisplay)
        val ivDetailThread = view.findViewById<ImageView>(R.id.ivDetailThread)
        val tvThreadNameDisplay = view.findViewById<TextView>(R.id.tvThreadNameDisplay)
        val btnBottomAction = view.findViewById<MaterialButton>(R.id.btnBottomAction)

        // 1. 뒤로가기 및 하단 버튼 액션 설정
        val backAction = View.OnClickListener {
            parentFragmentManager.popBackStack()
        }
        btnBack.setOnClickListener(backAction)
        btnBottomAction.setOnClickListener(backAction)

        // 2. 날짜 표시 포맷 변경 (2026-07-13 -> 2026년 07월 13일)
        selectedDate?.let { date ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                val parsedDate = inputFormat.parse(date)
                if (parsedDate != null) {
                    tvDateDisplay.text = outputFormat.format(parsedDate)
                } else {
                    tvDateDisplay.text = date
                }
            } catch (e: Exception) {
                tvDateDisplay.text = date
            }

            // 3. 데이터베이스에서 그날 획득한 실 데이터 가져오기
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(requireContext())
                val session = db.sessionDao().getSessionsByMonth(date.substring(0, 7))
                    .find { it.date == date && it.hasThread }

                session?.let {
                    val colorName = it.threadColorName
                    if (!colorName.isNullOrEmpty()) {
                        // 실 이름 띄우기
                        tvThreadNameDisplay.text = colorName

                        // 실 이미지 띄우기
                        val imageRes = ThreadColorManager.getDrawableByColorName(colorName)
                        ivDetailThread.setImageResource(imageRes)
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_DATE = "selected_date"

        fun newInstance(date: String) =
            ThreadDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DATE, date)
                }
            }
    }
}