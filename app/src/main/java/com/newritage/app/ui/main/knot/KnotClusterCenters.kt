package com.newritage.app.ui.main.knot

import io.github.sceneview.math.Position

/**
 * 매듭 타입별(GLB object-space) 공간 클러스터 중심 좌표. 매듭 타입당 1회, 오프라인 k-means(k=7,
 * k-means++ 초기화, 6회 재시작 중 최소 inertia 채택)로 계산해 상수로 고정했다. 계산 방법:
 * 1) Draco 디코딩한 원본 GLB의 POSITION을 위치 기준으로 중복 제거(같은 지점에 노멀만 다른
 *    정점을 하나로 병합)한 뒤 k-means 클러스터링.
 * 2) 클러스터 중심들을 그 중심들 자체의 최대 분산 축(스컬핑 메시라 끈을 따라간 순서 정보가
 *    없어 "냉색→온색" 축을 원본대로 복원할 수 없으므로, 차선책으로 중심들이 가장 넓게
 *    퍼진 축을 정렬 기준으로 채택) 기준 오름차순 정렬 — [KnotClusterColorMapping]이 스펙트럼
 *    순으로 배분한 색 리스트를 이 순서 그대로 얹으면 대략적인 냉색→온색 흐름이 재현된다.
 *
 * 좌표계 주의: 이 좌표는 KnotModelViewer의 ModelNode가 적용하는 scaleToUnits/centerOrigin/회전
 * 등 어떤 런타임 변환도 거치지 않은 "원본 GLB object-space" 값이다. 커스텀 머티리얼
 * (knot_cluster.mat)의 프래그먼트 셰이더도 world position이 아니라 정점 단계의 원본
 * mesh_position(object-space)을 varying으로 그대로 넘겨받아 비교하므로 좌표계가 일치한다.
 * world position을 쓰면 scaleToUnits·회전 때문에 색이 엉뚱한 위치에 칠해진다.
 */
object KnotClusterCenters {
    /** 모든 매듭 타입이 공유하는 클러스터 개수. knot_cluster.mat의 유니폼 배열 크기와 일치해야 한다. */
    const val CLUSTER_COUNT = 7

    // 정렬 축: X (클러스터 중심들이 가장 넓게 퍼진 축)
    val GARAKJI = listOf(
        Position(-0.097277f, -0.000024f, -0.131870f),
        Position(-0.011139f, 0.005347f, -0.056701f),
        Position(-0.007651f, 0.002009f, -0.005184f),
        Position(0.034605f, -0.000173f, 0.025017f),
        Position(0.041191f, -0.000649f, -0.061464f),
        Position(0.065520f, 0.004485f, -0.008381f),
        Position(0.155781f, 0.000083f, 0.038997f),
    )

    // 정렬 축: Z (클러스터 중심들이 가장 넓게 퍼진 축)
    val GUKHWA = listOf(
        Position(0.022120f, -0.017659f, -0.091448f),
        Position(-0.022799f, -0.011277f, -0.052015f),
        Position(0.048574f, -0.011999f, -0.005536f),
        Position(-0.076019f, -0.021111f, -0.004138f),
        Position(-0.020380f, -0.017306f, 0.016542f),
        Position(-0.021788f, -0.019803f, 0.068576f),
        Position(-0.033975f, -0.036305f, 0.149320f),
    )

    // 정렬 축: Z (클러스터 중심들이 가장 넓게 퍼진 축)
    val DORAE = listOf(
        Position(0.025970f, 0.000019f, -0.097280f),
        Position(0.036019f, -0.002060f, -0.039988f),
        Position(0.011540f, -0.009052f, -0.036600f),
        Position(0.017380f, 0.016145f, -0.034571f),
        Position(0.025772f, 0.000007f, -0.003762f),
        Position(0.000420f, -0.000027f, 0.030971f),
        Position(0.048100f, -0.000602f, 0.041779f),
    )

    // 정렬 축: Z (클러스터 중심들이 가장 넓게 퍼진 축)
    val MAEHWA = listOf(
        Position(-0.010317f, 0.000081f, -0.070114f),
        Position(0.011598f, 0.000154f, -0.025577f),
        Position(0.054416f, -0.000025f, -0.021927f),
        Position(-0.012936f, 0.000110f, -0.018114f),
        Position(-0.056935f, 0.000009f, -0.012722f),
        Position(0.004689f, -0.000030f, 0.030408f),
        Position(-0.002862f, 0.000053f, 0.099777f),
    )

    // 정렬 축: X (클러스터 중심들이 가장 넓게 퍼진 축)
    val SAMJEONGJA = listOf(
        Position(-0.087610f, 0.003612f, -0.026082f),
        Position(-0.079986f, 0.004352f, 0.008485f),
        Position(-0.006962f, 0.005015f, -0.004736f),
        Position(-0.006670f, 0.000239f, 0.126407f),
        Position(-0.006382f, 0.000439f, 0.047693f),
        Position(0.066791f, 0.004285f, 0.008335f),
        Position(0.085648f, 0.003483f, -0.022478f),
    )

    // 정렬 축: Z (클러스터 중심들이 가장 넓게 퍼진 축)
    val SAENGJJOK = listOf(
        Position(0.006385f, 0.000181f, -0.050950f),
        Position(-0.035430f, -0.000035f, -0.030712f),
        Position(0.053464f, -0.000022f, -0.019543f),
        Position(0.018664f, 0.000027f, -0.018866f),
        Position(-0.006322f, -0.000470f, -0.018551f),
        Position(0.005074f, 0.000008f, 0.035358f),
        Position(0.004180f, 0.000085f, 0.085477f),
    )

    // 정렬 축: Z (클러스터 중심들이 가장 넓게 퍼진 축)
    val ANGYEONG = listOf(
        Position(0.000675f, -0.000037f, -0.094425f),
        Position(-0.009712f, 0.007422f, -0.034735f),
        Position(0.017901f, 0.005996f, -0.021173f),
        Position(-0.013166f, 0.006822f, -0.000272f),
        Position(0.014925f, -0.000601f, 0.008085f),
        Position(0.001421f, -0.000034f, 0.072047f),
        Position(0.001347f, 0.000058f, 0.135922f),
    )

    // 정렬 축: X (클러스터 중심들이 가장 넓게 퍼진 축)
    val NABI = listOf(
        Position(-414.701865f, -6.293917f, -29.932009f),
        Position(-338.603295f, -7.669859f, 46.716766f),
        Position(-259.901723f, -11.954966f, 155.434633f),
        Position(-253.167046f, -12.096651f, 76.157302f),
        Position(-252.192652f, -13.102347f, -12.442952f),
        Position(-174.988354f, -7.980697f, 50.573487f),
        Position(-81.503914f, -8.626529f, -19.500651f),
    )

    // 정렬 축: Z (클러스터 중심들이 가장 넓게 퍼진 축)
    val BYEONGARI = listOf(
        Position(-511.673141f, -12.386869f, -63.135094f),
        Position(-471.115371f, -12.939559f, 8.239604f),
        Position(-611.510676f, -11.242271f, 9.058541f),
        Position(-540.035368f, -9.819387f, 24.449635f),
        Position(-402.940034f, -12.011862f, 31.245715f),
        Position(-508.650396f, -10.723704f, 101.474760f),
        Position(-514.663899f, -2.526559f, 244.864498f),
    )

    // 정렬 축: Z (클러스터 중심들이 가장 넓게 퍼진 축)
    val GAJIBANGSEOK = listOf(
        Position(-595.142827f, -9.426525f, 6.398892f),
        Position(-537.052834f, -12.829310f, 6.518895f),
        Position(-646.676840f, -13.107881f, 65.124288f),
        Position(-573.961240f, -13.435941f, 66.204380f),
        Position(-511.288997f, -11.862647f, 83.469301f),
        Position(-589.445725f, -13.914079f, 125.916693f),
        Position(-583.400723f, -1.826079f, 221.827745f),
    )

    /** [com.newritage.app.data.KnotType.assetPath]의 파일명(확장자 제외)으로 클러스터 중심 목록을 찾는다. */
    fun forAssetPath(assetPath: String): List<Position> =
        when (assetPath.substringAfterLast('/').removeSuffix(".glb")) {
            "garakji" -> GARAKJI
            "gukhwa" -> GUKHWA
            "dorae" -> DORAE
            "maehwa" -> MAEHWA
            "samjeongja" -> SAMJEONGJA
            "saengjjok" -> SAENGJJOK
            "angyeong" -> ANGYEONG
            "nabi" -> NABI
            "byeongari" -> BYEONGARI
            "gajibangseok" -> GAJIBANGSEOK
            else -> GARAKJI
        }
}
