package com.newritage.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayoutMediator
import com.newritage.app.R
import com.newritage.app.data.UserPreferences
import com.newritage.app.databinding.ActivityOnboardingBinding
import com.newritage.app.ui.auth.StartActivity
import com.newritage.app.ui.util.ShadowedImageView


class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: UserPreferences

    private val pages = listOf(
        OnboardingPage(R.string.onboarding_title_1, R.drawable.logo, 140, withShadow = true),
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
    // 화면을 세로 중앙선 기준 좌/우로 나눠, 오른쪽을 누르면 다음 페이지로,
    // 왼쪽을 누르면 이전 페이지로 이동한다.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_UP) {
            val current = binding.viewPager.currentItem
            val isRightHalf = ev.x >= binding.root.width / 2f

            if (isRightHalf) {
                if (current < pages.size - 1) {
                    binding.viewPager.currentItem = current + 1
                } else {
                    // 마지막 페이지면 로그인/시작 화면으로 이동
                    goToStart()
                }
            } else if (current > 0) {
                binding.viewPager.currentItem = current - 1
            }
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}

data class OnboardingPage(
    val titleResId: Int,
    val imageResId: Int,
    val imageHeightDp: Int,   // 페이지별 이미지 높이(dp)
    val withShadow: Boolean = false
)

class OnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvOnboardingTitle)
        val ivIcon: ShadowedImageView = view.findViewById(R.id.ivOnboardingIcon)
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
        holder.ivIcon.isShadowEnabled = page.withShadow

        // 여기부터 추가하는 3줄
        val density = holder.itemView.context.resources.displayMetrics.density
        val heightPx = (page.imageHeightDp * density).toInt()
        val lp = holder.ivIcon.layoutParams
        lp.height = heightPx
        holder.ivIcon.layoutParams = lp
    }

    override fun getItemCount() = pages.size
}
