package com.newritage.app.ble

import kotlin.random.Random

/**
 * 실기기(ESP32)가 연결되지 않았을 때 쓰는 가상 센서 데이터 생성기.
 * 실제 사람이 명상 중 손에 가하는 압력처럼 자연스럽게 흔들리는 값을 만든다.
 * reset 계열 함수를 호출할 때마다 새 시드를 뽑으므로 측정할 때마다 다른 곡선이 나온다.
 */
class MockPressureSimulator {

    data class Sample(val f0: Int, val f1: Int, val f2: Int, val total: Int)

    private var random = Random(System.nanoTime())

    private var thumb = 0.0
    private var im = 0.0
    private var palm = 0.0

    private var thumbCenter = 0.0
    private var imCenter = 0.0
    private var palmCenter = 0.0

    // 세션 평균이 수렴해야 할 목표(1개 센서 기준)와, 그 목표 둘레에서 얼마나 출렁일지.
    private var sessionTargetPerSensor = 0.0
    private var volatility = 1.0
    private var driftWalk = 0.0

    private var spikeTicksLeft = 0
    private var spikeStrength = 0.0

    /** 기준선 측정용 리셋: 가벼운 접촉 압력대에서 잔잔하게만 흔든다. */
    fun resetForBaseline() {
        random = Random(System.nanoTime())
        thumbCenter = random.nextDouble(300.0, 1600.0)
        imCenter = random.nextDouble(300.0, 1600.0)
        palmCenter = random.nextDouble(300.0, 1600.0)
        thumb = thumbCenter
        im = imCenter
        palm = palmCenter
        spikeTicksLeft = 0
        volatility = 1.0
        driftWalk = 0.0
    }

    /**
     * 명상 세션용 리셋. [ThreadColors.assignColor]가 세션 평균을 baseline 대비 ±20%로 나눠
     * 낮음/보통/높음을 정하므로, 매 세션마다 그 세 구간 중 하나를 균등 확률로 뽑고(경계에서
     * 노이즈에 밀리지 않도록 여유를 둔 비율 범위 안에서) 목표를 정한다. 그래서 매 명상마다
     * 낮음/보통/높음이 골고루 나온다. 그날의 "변동성"(잔잔한 날~들쑥날쑥한 날)도 세션마다
     * 새로 뽑아, 같은 목표 구간이라도 하루하루 곡선의 느낌이 달라지게 한다.
     */
    fun resetForSession(baselineTotal: Int) {
        random = Random(System.nanoTime())
        val baselinePerSensor = baselineTotal.coerceAtLeast(300) / 3.0

        val levelRatio = when (random.nextInt(3)) {
            0 -> random.nextDouble(-0.45, -0.25) // 낮음 (<= -20%)
            1 -> random.nextDouble(-0.15, 0.15)  // 보통 (-20% ~ 20%)
            else -> random.nextDouble(0.25, 0.55) // 높음 (>= 20%)
        }
        sessionTargetPerSensor = baselinePerSensor * (1.0 + levelRatio)

        // 0.5(잔잔한 날) ~ 2.4(들쑥날쑥한 날) 사이에서 매 세션 랜덤으로 정해진다.
        volatility = random.nextDouble(0.5, 2.4)
        driftWalk = 0.0

        thumbCenter = sessionTargetPerSensor * random.nextDouble(0.9, 1.1)
        imCenter = sessionTargetPerSensor * random.nextDouble(0.9, 1.1)
        palmCenter = sessionTargetPerSensor * random.nextDouble(0.9, 1.1)
        thumb = thumbCenter
        im = imCenter
        palm = palmCenter
        spikeTicksLeft = 0
    }

    /** 매 tick(호출 주기는 호출부의 TICK_INTERVAL_MS)마다 다음 샘플을 만든다. */
    fun next(isSession: Boolean): Sample {
        if (isSession) {
            // 목표 구간 중심을 기준으로 천천히 흔들리는 평균회귀 랜덤워크.
            // volatility가 클수록 흔들림 폭이 커져 "들쑥날쑥한 날"이 만들어진다.
            val driftMaxAmplitude = sessionTargetPerSensor.coerceAtLeast(50.0) * 0.35 * volatility
            driftWalk += random.nextDouble(-1.0, 1.0) * driftMaxAmplitude * 0.03
            driftWalk -= driftWalk * 0.01
            driftWalk = driftWalk.coerceIn(-driftMaxAmplitude, driftMaxAmplitude)

            thumbCenter = sessionTargetPerSensor + driftWalk
            imCenter = sessionTargetPerSensor + driftWalk
            palmCenter = sessionTargetPerSensor + driftWalk

            // volatility가 클수록 순간적인 스파이크(위/아래 모두)가 더 자주 발생한다.
            if (spikeTicksLeft <= 0 && random.nextDouble() < 0.003 * volatility) {
                spikeTicksLeft = random.nextInt(10, 35)
                val sign = if (random.nextBoolean()) 1.0 else -1.0
                spikeStrength = sign * sessionTargetPerSensor.coerceAtLeast(50.0) *
                    random.nextDouble(0.2, 0.5) * volatility
            }
        }

        val spike = if (spikeTicksLeft > 0) {
            spikeTicksLeft--
            spikeStrength
        } else {
            0.0
        }

        val noiseScale = if (isSession) volatility else 1.0
        thumb = wander(thumb, thumbCenter + spike * 0.3, noiseScale)
        im = wander(im, imCenter + spike * 0.4, noiseScale)
        palm = wander(palm, palmCenter + spike * 0.3, noiseScale)

        val f0 = thumb.toInt().coerceIn(0, 4095)
        val f1 = im.toInt().coerceIn(0, 4095)
        val f2 = palm.toInt().coerceIn(0, 4095)
        return Sample(f0, f1, f2, f0 + f1 + f2)
    }

    /** 목표 중심값 쪽으로 이끌리면서 작은 노이즈로 흔들리는 1-step 랜덤워크. */
    private fun wander(current: Double, center: Double, noiseScale: Double): Double {
        val pull = (center - current) * 0.08
        val noise = random.nextDouble(-1.0, 1.0) * (center.coerceAtLeast(50.0) * 0.03) * noiseScale
        return (current + pull + noise).coerceIn(0.0, 4095.0)
    }
}
