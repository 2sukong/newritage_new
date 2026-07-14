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
import java.util.Calendar
import java.util.Locale

class ThreadDetailFragment : Fragment() {

    private var selectedDate: String? = null

    // 날짜 포맷 정의 (상수로 관리)
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())

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

        // [추가] 어제/내일 이동을 위한 화살표 버튼 매핑 (레이아웃 ID에 맞게 수정하세요)
        val btnPrevDate = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNextDate = view.findViewById<ImageButton>(R.id.btnNextMonth)

        // 1. 뒤로가기 및 하단 버튼 액션 설정
        val backAction = View.OnClickListener {
            parentFragmentManager.popBackStack()
        }
        btnBack.setOnClickListener(backAction)
        btnBottomAction.setOnClickListener(backAction)

        // 2. 어제/내일 이동 버튼 이벤트 리스너 설정
        btnPrevDate.setOnClickListener {
            changeDate(daysToAdd = -1, tvDateDisplay, tvThreadNameDisplay, ivDetailThread)
        }
        btnNextDate.setOnClickListener {
            changeDate(daysToAdd = 1, tvDateDisplay, tvThreadNameDisplay, ivDetailThread)
        }

        // 3. 최초 진입 시 데이터 로드 및 UI 업데이트
        selectedDate?.let { date ->
            updateUI(date, tvDateDisplay, tvThreadNameDisplay, ivDetailThread)
        }
    }

    /**
     * 날짜를 계산하고 UI를 갱신하는 메서드
     * [daysToAdd]에 -1을 넣으면 어제, 1을 넣으면 내일로 계산합니다.
     */
    private fun changeDate(
        daysToAdd: Int,
        tvDateDisplay: TextView,
        tvThreadNameDisplay: TextView,
        ivDetailThread: ImageView
    ) {
        val currentDateStr = selectedDate ?: return
        try {
            val date = apiDateFormat.parse(currentDateStr) ?: return
            val calendar = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, daysToAdd)
            }

            // 변경된 날짜를 "yyyy-MM-dd" 형식의 String으로 갱신
            val newDateStr = apiDateFormat.format(calendar.time)
            selectedDate = newDateStr

            // 바뀐 날짜로 데이터 조회 및 UI 변경
            updateUI(newDateStr, tvDateDisplay, tvThreadNameDisplay, ivDetailThread)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 특정 날짜의 데이터를 데이터베이스에서 가져와 화면에 그리는 메서드
     */
    private fun updateUI(
        date: String,
        tvDateDisplay: TextView,
        tvThreadNameDisplay: TextView,
        ivDetailThread: ImageView
    ) {
        // 1. 상단 날짜 텍스트 변경
        try {
            val parsedDate = apiDateFormat.parse(date)
            if (parsedDate != null) {
                tvDateDisplay.text = displayDateFormat.format(parsedDate)
            } else {
                tvDateDisplay.text = date
            }
        } catch (e: Exception) {
            tvDateDisplay.text = date
        }

        // 2. 만약 해당 날짜에 데이터가 없을 때를 대비한 초기화 작업
        tvThreadNameDisplay.text = "기록된 실이 없습니다" // 기본 텍스트
        ivDetailThread.setImageResource(0) // 이미지 비우기 (또는 기본 플레이스홀더 이미지)

        // 3. 코루틴을 통해 해당 날짜 데이터베이스 조회
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            // 월 단위로 전체 조회를 하므로 date에서 yyyy-MM 부분 추출
            val monthStr = if (date.length >= 7) date.substring(0, 7) else date
            val session = db.sessionDao().getSessionsByMonth(monthStr)
                .find { it.date == date && it.hasThread }

            session?.let {
                val colorName = it.threadColorName
                if (!colorName.isNullOrEmpty()) {
                    // 실 이름 및 이미지 갱신
                    tvThreadNameDisplay.text = colorName
                    val imageRes = ThreadColorManager.getDrawableByColorName(colorName)
                    ivDetailThread.setImageResource(imageRes)
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