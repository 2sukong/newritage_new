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
import com.google.android.material.tabs.TabLayoutMediator
import com.newritage.app.R
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityOnboardingBinding
import com.newritage.app.ui.auth.StartActivity


class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: UserPreferences

    private val pages = listOf(
        OnboardingPage(R.string.onboarding_title_1, R.drawable.logo, 140),
        OnboardingPage(R.string.onboarding_title_2, R.drawable.onboarding1, 200),
        OnboardingPage(R.string.onboarding_title_3, R.drawable.onboarding2, 240),
        OnboardingPage(R.string.onboarding_title_4, R.drawable.onboarding3, 180)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = UserPreferences(this)

        setupViewPager()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingAdapter(pages)

        TabLayoutMediator(binding.tabIndicator, binding.viewPager) { _, _ -> }.attach()


    }


    private fun goToStart() {
        prefs.isOnboardingDone = true
        startActivity(Intent(this, StartActivity::class.java))
        finish()
    }
    // 화면 아무 데나 터치했을 때 다음 온보딩 페이지로 넘기는 함수 추가
    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        // 손가락을 화면에서 뗄 때(ACTION_UP) 실행
        if (ev?.action == android.view.MotionEvent.ACTION_UP) {
            val current = binding.viewPager.currentItem

            if (current < pages.size - 1) {
                // 마지막 페이지가 아니면 다음 페이지로 이동
                binding.viewPager.currentItem = current + 1
            } else {
                // 마지막 페이지면 로그인/시작 화면으로 이동
                goToStart()
            }
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}

data class OnboardingPage(
    val titleResId: Int,
    val imageResId: Int,
    val imageHeightDp: Int   // 페이지별 이미지 높이(dp)
)

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
        val page = pages[position]

        holder.tvTitle.setText(page.titleResId)
        holder.ivIcon.setImageResource(page.imageResId)

        val density = holder.itemView.context.resources.displayMetrics.density
        val heightPx = (page.imageHeightDp * density).toInt()
        val lp = holder.ivIcon.layoutParams
        lp.height = heightPx
        holder.ivIcon.layoutParams = lp

        if (position == 0) {
            holder.ivIcon.outlineProvider = android.view.ViewOutlineProvider.BOUNDS
            holder.ivIcon.elevation = 12f
        } else {
            holder.ivIcon.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            holder.ivIcon.elevation = 0f
        }
    }

    override fun getItemCount() = pages.size
}
