package com.newritage.app.ui.main.knot

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KnotDetailFragment : Fragment() {

    private var currentDate = Calendar.getInstance()
    private val dao by lazy { AppDatabase.getInstance(requireContext()).sessionDao() }
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displaySdf = SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault())

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
        val ivKnotImage = view.findViewById<ImageView>(R.id.ivKnotImage)
        val tvKnotName = view.findViewById<TextView>(R.id.tvKnotNameDisplay)
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
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            loadKnotData(tvDate, ivKnotImage, tvKnotName)
        }

        btnNext.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, 1)
            loadKnotData(tvDate, ivKnotImage, tvKnotName)
        }

        loadKnotData(tvDate, ivKnotImage, tvKnotName)
    }

    private fun loadKnotData(tvDate: TextView, ivKnotImage: ImageView, tvKnotName: TextView) {
        val dateStr = sdf.format(currentDate.time)
        tvDate.text = displaySdf.format(currentDate.time)

        lifecycleScope.launch {
            val session = dao.getThreadSessionByDate(dateStr)
            if (session != null) {
                tvKnotName.text = "${session.threadColorName.ifEmpty { "매듭" }}"
                try {
                    ivKnotImage.setBackgroundColor(Color.parseColor(session.threadColor))
                } catch (e: IllegalArgumentException) {
                    ivKnotImage.setBackgroundColor(Color.parseColor("#CADCC4"))
                }
            } else {
                tvKnotName.text = "기록된 매듭이 없습니다"
                ivKnotImage.setBackgroundColor(Color.TRANSPARENT)
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
