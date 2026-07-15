package com.newritage.app.ui.main.knot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.newritage.app.R
import com.newritage.app.data.AppDatabase
import com.newritage.app.data.KnotType
import kotlinx.coroutines.delay
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
        val knotComposeViewer = view.findViewById<ComposeView>(R.id.knotComposeViewer)
        val tvKnotName = view.findViewById<TextView>(R.id.tvKnotNameDisplay)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextMonth)
        val btnBottomAction = view.findViewById<Button>(R.id.btnBottomAction)

        knotComposeViewer.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnBottomAction.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnPrev.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            loadKnotData(tvDate, knotComposeViewer, tvKnotName)
        }

        btnNext.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, 1)
            loadKnotData(tvDate, knotComposeViewer, tvKnotName)
        }

        // 그리드 화면의 3D 썸네일이 완전히 정리되기 전에 상세보기의 3D 뷰가 같은 프레임에서 만들어지면
        // Filament 렌더 스레드가 네이티브 크래시를 낸다. 화면 전환 애니메이션이 끝날 시간을 잠깐 준다.
        viewLifecycleOwner.lifecycleScope.launch {
            delay(300)
            loadKnotData(tvDate, knotComposeViewer, tvKnotName)
        }
    }

    private fun loadKnotData(tvDate: TextView, knotComposeViewer: ComposeView, tvKnotName: TextView) {
        val dateStr = sdf.format(currentDate.time)
        tvDate.text = displaySdf.format(currentDate.time)

        lifecycleScope.launch {
            val session = dao.getThreadSessionByDate(dateStr)
            if (session != null) {
                val knotType = KnotType.forDate(dateStr)
                tvKnotName.text = "${session.threadColorName.ifEmpty { "매듭" }} · ${knotType.displayName}"
                // 상세보기: 위치 이동(pan)은 막고 회전(orbit)만 가능하게 한다.
                knotComposeViewer.setContent {
                    KnotModelViewer(
                        glbAssetPath = knotType.assetPath,
                        interactive = true,
                        tintColor = knotTintColorOrNull(session.threadColor),
                        // 그리드 썸네일(기본 3f)보다 카메라를 가깝게 둬서 상세보기에서 매듭이 약 2배
                        // 크게 보이도록 한다.
                        cameraDistance = 1.5f,
                        // 3D 모델을 GLB에서 읽어 GPU에 올리는 동안 빈 화면 대신 그리드용 정적
                        // 썸네일을 먼저 보여줘 로딩 지연을 체감상 없앤다.
                        placeholderRes = knotType.thumbnailRes
                    )
                }
            } else {
                tvKnotName.text = "기록된 매듭이 없습니다"
                knotComposeViewer.setContent {}
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
