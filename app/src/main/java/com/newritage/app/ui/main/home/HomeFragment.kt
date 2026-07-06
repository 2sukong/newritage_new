package com.newritage.app.ui.main.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.newritage.app.R
import com.newritage.app.databinding.FragmentHomeBinding
import com.newritage.app.ui.measurement.MeasurementActivity

class HomeFragment : Fragment() {

    private enum class MeasureMode { AUTO, GUIDE }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var currentMode = MeasureMode.AUTO

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgMeasureStart.setOnClickListener {
            startActivity(Intent(requireContext(), MeasurementActivity::class.java).apply {
                putExtra("auto_start", true)
            })
        }

        binding.btnAutoMode.setOnClickListener {
            currentMode = MeasureMode.AUTO
            updateModeUI()
        }
        binding.btnGuideMode.setOnClickListener {
            currentMode = MeasureMode.GUIDE
            updateModeUI()
        }
        updateModeUI()
    }

    private fun updateModeUI() {
        when (currentMode) {
            MeasureMode.AUTO -> {
                binding.btnAutoMode.setBackgroundResource(R.drawable.bg_mode_selected)
                binding.btnAutoMode.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

                binding.btnGuideMode.setBackgroundResource(R.drawable.bg_mode_unselected)
                binding.btnGuideMode.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            MeasureMode.GUIDE -> {
                binding.btnGuideMode.setBackgroundResource(R.drawable.bg_mode_selected)
                binding.btnGuideMode.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

                binding.btnAutoMode.setBackgroundResource(R.drawable.bg_mode_unselected)
                binding.btnAutoMode.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
