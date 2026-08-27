package com.newritage.app.ui.main

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.newritage.app.R
import com.newritage.app.databinding.DialogHelpImageBinding

/**
 * 도움말 카드를 띄우는 팝업.
 * 카드/텍스트는 모두 네이티브 뷰(반투명 배경 + TextView)로 구성되어 있고(2026-08-26,
 * 기존 help.webp 래스터 이미지 방식에서 전환), 뒤에 있던 실제 화면이 카드 안으로도
 * 은은하게 비쳐 보인다(반투명). 다이얼로그 윈도우 자체를 화면 전체가 아니라 "카드 +
 * 그림자 여백" 크기로만 잡는다(applyCardWindowBounds() 참고, 2026-08-26 6차 개편 —
 * "카드가 겹치는 부분만 불투명해 보이고 나머지 화면은 완전히 투명하게 보이도록" 요청
 * 반영). API 31(Android 12) 이상에서는 그 작은 윈도우 자신에게만 배경 블러(Window.setBackgroundBlurRadius)를 적용해
 * 카드 부분만 자연스럽게 흐려 보이게 하고, 그 미만 기기(minSdk 26)에서는 블러 없이 카드
 * 자신의 반투명 배경(45% 알파)만으로 뒤 화면이 옅게 비쳐 보인다(흐려 보이진 않음, 순수
 * 알파 블렌딩). 카드 바깥(그림자 여백) 터치 또는 윈도우 바깥 터치 시 닫힌다.
 */
class HelpImageDialog(private val hostActivity: Activity) : Dialog(hostActivity) {

    private lateinit var binding: DialogHelpImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogHelpImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        applyCardWindowBounds()
        window?.setWindowAnimations(R.style.Animation_HelpImageDialog)
        setCanceledOnTouchOutside(true) // 윈도우가 카드 크기로 작아졌으니, 윈도우 바깥(화면 대부분)을
        // 눌러도 확실히 닫히도록 명시(2026-08-26). 기본값도 true라 원래도 동작했겠지만 명시해서 보장.
        // 다이얼로그 기본 dim(화면 전체를 어둡게 깔아 카드를 강조하는 기본 동작)을 끈다.
        // 이제 윈도우 자체가 카드 크기라 기본 dim을 켜두면 그 작은 윈도우 영역만 이상하게
        // 어두워지고 화면 나머지는 그대로라 더 부자연스러움 — 카드 바깥은 항상 그대로 선명하게
        // 두는 게 의도이므로 dim은 완전히 끔.
        window?.setDimAmount(0f)
        applyBackgroundBlurIfSupported()
        applyCardShadowBlurIfSupported()

        // 이 다이얼로그는 별도 Window라 상태바 색이 기본값으로 리셋될 수 있다.
        // 앱 테마와 같은 상단바 색/아이콘 밝기를 그대로 유지한다.
        window?.let { win ->
            win.statusBarColor = ContextCompat.getColor(hostActivity, R.color.background)
            WindowInsetsControllerCompat(win, win.decorView).isAppearanceLightStatusBars = true
        }

        // 카드 바깥(배경) 터치 시 닫기. 카드 자체는 클릭을 소비해 닫히지 않는다.
        binding.helpDialogRoot.setOnClickListener { dismiss() }
        binding.helpCard.setOnClickListener { }
    }

    /**
     * 다이얼로그 윈도우 자체를 화면 전체가 아니라 "카드 + 그림자 여백" 크기로만 잡고,
     * 화면 안에서 카드가 있어야 할 위치로 옮긴다(2026-08-26 6차 개편). 이렇게 하면
     * 배경 블러(바로 아래 applyBackgroundBlurIfSupported())가 이 작은 윈도우 자신에게만
     * 적용되어, 카드 바깥 화면은 전혀 흐려지거나 어두워지지 않고 그대로 선명하게 보인다 —
     * "카드가 겹치는 부분만 불투명해 보이고 나머지는 완전히 투명하게" 요청 반영.
     * 카드 폭은 화면 폭의 [CARD_WIDTH_FRACTION](기존 guideCardStart/End=0.08/0.92와 동일
     * 비율), 카드 상단은 화면 높이의 [CARD_TOP_FRACTION] 지점(2026-08-26, "카드 위치를
     * 더 아래로" 요청으로 기존 0.12에서 하향 조정), 카드 최대 높이는 화면 높이의
     * ([CARD_BOTTOM_FRACTION] - [CARD_TOP_FRACTION])만큼으로 계산한다(그 이상 내용이
     * 길어지면 dialog_help_image.xml의 `helpCard` ScrollView가 자동으로 스크롤). 여기에
     * 그림자가 카드 사방으로 삐져나올 [CARD_SHADOW_MARGIN_DP]만큼을 윈도우 폭/높이에 더
     * 얹고, 그만큼 위치도 보정해서 카드 자체는 항상 의도한 위치에 오도록 한다.
     * ⚠️ 이 클라우드 개발 환경엔 Android SDK/에뮬레이터가 없어 실기에서 카드 위치/크기가
     * 기대대로인지, 윈도우가 사각형이라 카드의 둥근 모서리 바깥(그림자 부분)에 블러가 살짝
     * 새어 보이는 정도가 거슬리지 않는지 확인하지 못했다 — 실기 확인 필요.
     */
    private fun applyCardWindowBounds() {
        val win = window ?: return
        val metrics = hostActivity.resources.displayMetrics
        val density = metrics.density
        val screenWidthPx = metrics.widthPixels.toFloat()
        val screenHeightPx = metrics.heightPixels.toFloat()
        val marginPx = CARD_SHADOW_MARGIN_DP * density

        val cardWidthPx = screenWidthPx * CARD_WIDTH_FRACTION
        val cardTopPx = screenHeightPx * CARD_TOP_FRACTION
        val cardMaxHeightPx = screenHeightPx * (CARD_BOTTOM_FRACTION - CARD_TOP_FRACTION)

        val windowWidthPx = (cardWidthPx + marginPx * 2).toInt()
        val windowHeightPx = (cardMaxHeightPx + marginPx * 2).toInt()
        val windowX = (((screenWidthPx - cardWidthPx) / 2f) - marginPx).toInt()
        val windowY = (cardTopPx - marginPx).toInt()

        win.setGravity(Gravity.TOP or Gravity.START)
        win.setLayout(windowWidthPx, windowHeightPx)
        win.attributes = win.attributes.apply {
            x = windowX
            y = windowY
        }
    }

    /**
     * Android 12(API 31)부터 제공되는 "윈도우 자신의 배경 블러"를 적용한다
     * (Window.setBackgroundBlurRadius). 2026-08-27 7차 개편 — 기존엔
     * WindowManager.LayoutParams.FLAG_BLUR_BEHIND + blurBehindRadius 조합을 썼는데,
     * 다이얼로그 윈도우 자체를 카드+그림자 여백 크기로 작게 잡아뒀는데도 블러가 화면
     * 전체에 걸리는 문제가 있었다("2. 화면 전체를 덮는 반투명 흰색과 블러", 2026-08-27
     * 피드백) — FLAG_BLUR_BEHIND는 "이 윈도우 뒤의 아래쪽 윈도우들"을 흐리는 구조라,
     * 요청 윈도우 자신의 실제 크기/모양과 무관하게 넓게(기기에 따라 화면 전체 기준으로)
     * 적용될 수 있는 것으로 보인다. setBackgroundBlurRadius()는 반대로 "이 윈도우 자신의
     * 배경"에 블러를 적용하는 API라, 블러 영역이 이 윈도우 자신의 사각형 범위
     * (=카드+그림자 여백 박스, 화면 전체가 아님)로 한정된다 — 화면 전체가 흐려지던
     * 문제의 직접적인 원인으로 추정되는 부분을 고쳤다.
     * 다만 이 API도 "윈도우의 사각형 범위" 안에서 투명한 부분에 블러를 보여주는 방식이라,
     * 카드의 둥근 모서리 딱 그 모양대로 블러를 완전히 잘라내는 것(그 바깥 그림자 여백에는
     * 전혀 블러가 없게)까지는 지원하지 않는다 — 그림자가 카드 바깥으로 삐져나와야 해서
     * 그림자와 카드를 같은 윈도우 안에 둘 수밖에 없는데, 윈도우 자체를 카드의 둥근
     * 모양으로 잘라내면(clipToOutline) 그 바깥의 그림자까지 함께 잘려서 안 보이게 되기
     * 때문이다. 대신 이번에 그림자 여백 자체를 크게 줄여서(bg_help_card_shadow_ring.xml
     * 참고, "3. 그림자 테두리 간격 줄이기") 카드의 둥근 모서리 바깥 네 귀퉁이에 블러가
     * 살짝 새어 보일 수 있는 영역 자체를 최소화했다 — 여기서 더 완전히 잘라내려면 카드용
     * 윈도우와 그림자용 윈도우를 아예 분리하는(2-윈도우) 구조가 필요한데, 실기 확인 없이
     * 그렇게까지 바꾸는 건 무리라 판단해 이번엔 적용하지 않았다.
     * isCrossWindowBlurEnabled() 체크는 기기가 크로스 윈도우 블러를 지원/허용하지 않을 때
     * (예: 절전 모드, 저사양 GPU, 시스템 설정으로 블러 끔) 블러 요청 자체를 하지 않기
     * 위함이다 — 이 체크 없이 blurRadius만 설정했을 때 일부 기기/모드에서 블러 대신
     * 정체불명의 회색 사각형(블러 미지원 시 시스템이 대신 보여주는 대체 스크림으로 추정)이
     * 나타났을 가능성이 있다고 보고 추가했다("4. 알 수 없는 회색 사각형", 2026-08-27
     * 피드백 대응).
     * API 31 미만(minSdk 26)에서는 애초에 이 함수가 아무것도 하지 않고 반환하므로, 블러
     * 없이 카드 자신의 반투명 배경(45% 알파)만으로 뒤 화면이 옅게 비쳐 보인다.
     * ⚠️ 이 클라우드 개발 환경엔 Android SDK/에뮬레이터가 없어 이 변경이 실기에서 두
     * 문제(전체 화면 블러, 회색 사각형)를 실제로 해결하는지, 카드 모서리 바깥 네 귀퉁이의
     * 잔여 블러 번짐이 거슬리지 않는 수준인지 확인하지 못했다 — 실기 확인 필요.
     */
    private fun applyBackgroundBlurIfSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val win = window ?: return
        if (!win.windowManager.isCrossWindowBlurEnabled) return
        val blurRadiusPx = (BLUR_RADIUS_DP * hostActivity.resources.displayMetrics.density).toInt()
        win.setBackgroundBlurRadius(blurRadiusPx)
    }

    /**
     * 도움말 카드 그림자(bg_help_card_shadow_ring.xml, `viewHelpCardShadow`)에 실제 가우시안
     * 블러를 적용한다. 이전엔 layer-list를 여러 겹 쌓아 블러를 흉내냈는데(각지고 얼룩덜룩해
     * 보인다는 피드백) + 카드가 반투명이라 도형이 카드 밑에 넓게 깔리면 카드 전체가 어둡게
     * 비쳐 보이는 문제가 있어서, "카드 경계 바로 바깥의 옅은 테두리 도형"(카드 밑에는 거의
     * 깔리지 않음) + View.setRenderEffect()의 실제 블러 조합으로 바꿈. RenderEffect는
     * API 31(Android 12)부터 제공되므로 그 미만에서는 아무것도 하지 않고 반환 — 이 경우
     * 블러 없는 옅은 테두리 도형만 그대로 보인다(각지긴 하지만 최소한 카드를 어둡게 만들진
     * 않는 안전한 폴백).
     * ⚠️ 이 클라우드 개발 환경엔 Android SDK/에뮬레이터가 없어 실기에서 블러 강도/번짐
     * 범위가 기대대로인지 확인하지 못했다 — 실기 확인 후 필요하면
     * CARD_SHADOW_BLUR_RADIUS_DP 값과 bg_help_card_shadow_ring.xml의 인셋/스트로크 두께를
     * 함께 조정할 것.
     */
    private fun applyCardShadowBlurIfSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val density = hostActivity.resources.displayMetrics.density
        val radiusPx = CARD_SHADOW_BLUR_RADIUS_DP * density
        binding.viewHelpCardShadow.setRenderEffect(
            RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
        )
    }

    companion object {
        private const val BLUR_RADIUS_DP = 10f // Figma "backdrop-filter: blur(10px)" 값 그대로 반영(2026-08-26)
        private const val CARD_SHADOW_BLUR_RADIUS_DP = 10f // 카드 그림자 자체 블러 반경(2026-08-26, 5차 조정 — 카드 안쪽까지 번지는 걸 줄이려 16f에서 축소)

        // 아래 4개는 다이얼로그 윈도우를 "카드 + 그림자 여백" 크기로만 잡기 위한 값
        // (applyCardWindowBounds(), 2026-08-26 6차 개편). CARD_SHADOW_MARGIN_DP는
        // dialog_help_image.xml의 helpCard/viewHelpCardShadow 마진, bg_help_card_shadow_ring
        // .xml의 인셋 계산과 반드시 같은 값으로 맞춰야 함(40dp) — 여기서 바꾸면 그 두 파일도
        // 같이 바꿀 것.
        private const val CARD_WIDTH_FRACTION = 0.84f // 화면 폭의 84% (기존 guideCardStart/End 0.08/0.92와 동일)
        private const val CARD_TOP_FRACTION = 0.20f // 카드 상단 = 화면 높이의 20% 지점(2026-08-26, "더 아래로" 요청으로 0.12에서 하향)
        private const val CARD_BOTTOM_FRACTION = 0.88f // 카드가 늘어날 수 있는 최대 하단 = 화면 높이의 88% 지점
        private const val CARD_SHADOW_MARGIN_DP = 40f // 카드 사방으로 그림자가 차지하는 여백
    }
}
