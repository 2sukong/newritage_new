package com.newritage.app.ui.main.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.newritage.app.databinding.FragmentAnalysisBinding

class AnalysisFragment : Fragment() {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rowDailyAnalysis.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.newritage.app.R.id.fragmentContainer, DailyAnalysisFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.rowWeeklyAnalysis.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.newritage.app.R.id.fragmentContainer, WeeklyAnalysisFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.rowMonthlyAnalysis.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.newritage.app.R.id.fragmentContainer, MonthlyAnalysisFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
