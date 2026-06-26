package com.newritage.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.newritage.app.R
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityOnboardingBinding
import com.newritage.app.ui.auth.StartActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: UserPreferences

    private val pages = listOf(
        OnboardingPage(R.string.onboarding_title_1),
        OnboardingPage(R.string.onboarding_title_2),
        OnboardingPage(R.string.onboarding_title_3),
        OnboardingPage(R.string.onboarding_title_4)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)

        setupViewPager()
        setupButtons()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingAdapter(pages)

        TabLayoutMediator(binding.tabIndicator, binding.viewPager) { _, _ -> }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
            }
        })
    }

    private fun setupButtons() {
        binding.btnSkip.setOnClickListener { goToStart() }
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.size - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                goToStart()
            }
        }
        updateButtons(0)
    }

    private fun updateButtons(position: Int) {
        if (position == pages.size - 1) {
            binding.btnNext.text = getString(R.string.onboarding_start)
            binding.btnSkip.visibility = View.INVISIBLE
        } else {
            binding.btnNext.text = getString(R.string.onboarding_next)
            binding.btnSkip.visibility = View.VISIBLE
        }
    }

    private fun goToStart() {
        prefs.isOnboardingDone = true
        startActivity(Intent(this, StartActivity::class.java))
        finish()
    }
}

data class OnboardingPage(val titleResId: Int)

class OnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvOnboardingTitle)
        val ivIcon: ImageView = view.findViewById(R.id.ivOnboardingIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_onboarding_page, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvTitle.setText(pages[position].titleResId)
        // 아이콘 이미지는 페이지별로 다를 수 있음 (현재 기본 아이콘 사용)
    }

    override fun getItemCount() = pages.size
}
