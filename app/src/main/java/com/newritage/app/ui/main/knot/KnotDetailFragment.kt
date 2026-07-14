package com.newritage.app.ui.main.knot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.ui.main.knot.recommend.DiaryEntry
import com.newritage.app.ui.main.knot.recommend.RecommendationEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 이 화면이 보여주는 매듭은 하루 단위가 아니라, 표시 중인 달 전체의 일기를 분석해 정해지는 "이달의 매듭"이다. */
class KnotDetailFragment : Fragment() {

    private var currentDate = Calendar.getInstance()
    private val dao by lazy { AppDatabase.getInstance(requireContext()).sessionDao() }
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val displaySdf = SimpleDateFormat("yyyy년 M월", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_DATE)?.let {
            try {
                currentDate.time = sdf.parse(it) ?: Date()
            } catch (e: Exception) {
                currentDate = Calendar.getInstance()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_knot_detail_view_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvDate = view.findViewById<TextView>(R.id.tvDateDisplay)
        val tvKnotName = view.findViewById<TextView>(R.id.tvKnotNameDisplay)
        val tvDescription = view.findViewById<TextView>(R.id.tvDescriptionText)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextMonth)
        val btnBottomAction = view.findViewById<Button>(R.id.btnBottomAction)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnBottomAction.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnPrev.setOnClickListener {
            currentDate.add(Calendar.MONTH, -1)
            loadKnotData(tvDate, tvKnotName, tvDescription)
        }

        btnNext.setOnClickListener {
            currentDate.add(Calendar.MONTH, 1)
            loadKnotData(tvDate, tvKnotName, tvDescription)
        }

        loadKnotData(tvDate, tvKnotName, tvDescription)
    }

    private fun loadKnotData(tvDate: TextView, tvKnotName: TextView, tvDescription: TextView) {
        val yearMonth = monthSdf.format(currentDate.time)
        tvDate.text = displaySdf.format(currentDate.time)

        lifecycleScope.launch {
            val sessions = dao.getSessionsByMonth(yearMonth)
            val entries = sessions
                .filter { it.emotion.isNotBlank() }
                .map { DiaryEntry(date = LocalDate.parse(it.date), content = it.emotion) }

            if (entries.isEmpty()) {
                tvKnotName.text = getString(R.string.no_knot_yet)
                tvDescription.text = ""
            } else {
                val knot = RecommendationEngine.recommendKnot(entries)
                tvKnotName.text = knot.name
                tvDescription.text = knot.meaning
            }
        }
    }

    companion object {
        private const val ARG_DATE = "arg_date"
        fun newInstance(date: String) = KnotDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_DATE, date) }
        }
    }
}
