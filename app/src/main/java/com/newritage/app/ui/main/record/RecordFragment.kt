package com.newritage.app.ui.main.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.newritage.app.data.AppDatabase
import com.newritage.app.databinding.FragmentRecordBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTodayRecord()
    }

    private fun loadTodayRecord() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val session = db.sessionDao().getLatestSessionByDate(today)

            if (session != null) {
                binding.layoutNoRecord.visibility = View.GONE
                binding.layoutRecord.visibility = View.VISIBLE

                val minutes = session.durationSeconds / 60
                val seconds = session.durationSeconds % 60
                binding.tvMedTime.text = String.format("%02d:%02d", minutes, seconds)
                binding.tvAvgPressure.text = String.format("%.1f kPa", session.avgPressure)
                binding.tvMaxPressure.text = String.format("%.1f kPa", session.maxPressure)
                binding.tvMinPressure.text = String.format("%.1f kPa", session.minPressure)
                binding.tvEmotion.text = if (session.emotion.isEmpty()) {
                    getString(com.newritage.app.R.string.no_emotion_yet)
                } else {
                    session.emotion
                }
            } else {
                binding.layoutNoRecord.visibility = View.VISIBLE
                binding.layoutRecord.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
