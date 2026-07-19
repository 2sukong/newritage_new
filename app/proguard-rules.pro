# Add project specific ProGuard rules here.

# ── 네이티브(JNI)로 이름 참조되는 클래스 보호 ─────────────────────────
# Filament/gltfio/utils는 C++ 네이티브에서 클래스·메서드를 이름으로 찾으므로
# R8이 이름을 바꾸거나 지우면 3D 뷰어가 런타임에 깨진다. 통째로 유지한다.
-keep class com.google.android.filament.** { *; }
-keep class com.google.android.filament.gltfio.** { *; }
-keep class com.google.android.filament.utils.** { *; }
-dontwarn com.google.android.filament.**

# SceneView (Filament 래퍼)
-keep class io.github.sceneview.** { *; }
-dontwarn io.github.sceneview.**

# 네이티브 메서드를 가진 클래스 멤버 유지(일반 안전장치)
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ── MPAndroidChart ─────────────────────────────────────────────────
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ── OkHttp / Okio (Gemini 연동) ────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
