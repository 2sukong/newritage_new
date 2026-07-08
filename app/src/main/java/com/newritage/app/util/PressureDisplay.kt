package com.newritage.app.util

/**
 * 원시 압력 total(0~12285, f0+f1+f2 이론상 최댓값 4095*3)을 화면 표시용 소형 스케일(0~60)로
 * 환산한다. 실제 물리 단위 변환은 아니며, 측정 화면(kPa)·기록 요약 화면(N) 등 여러 화면에서
 * 같은 축소 비율을 공유해 표시 숫자가 서로 어긋나지 않게 하기 위한 용도다.
 */
object PressureDisplay {
    const val SCALE = 12285f / 60f

    fun toDisplayValue(raw: Float): Float = raw / SCALE
}
