package com.newritage.app.ui.main.knot

import com.newritage.app.util.ThreadColors

/**
 * 그 달의 실 색 리스트(하루당 하나, 순서 무관)를 매듭의 공간 클러스터 개수(N)만큼의 대표색으로
 * 압축한다. 알고리즘은 애초에 정한 "네이티브 해상도 세그먼트 끈"의 B-밴드 폴백 규칙과 동일하다:
 * 1) 각 색의 등장 비율만큼 floor(비율 × N) 밴드를 먼저 배정
 * 2) 남는 자리는 소수부가 큰 색부터 하나씩(최대잉여법)
 * 3) 소수부 동률이면 스펙트럼 인덱스가 낮은(더 차가운) 색이 우선
 * 이렇게 정확히 N개가 되도록 밴드를 배정한 뒤, 냉색→온색 순으로 펼쳐 [KnotClusterCenters]가
 * 정렬해둔 클러스터 순서(중심 좌표가 가장 넓게 퍼진 축 기준 오름차순)에 그대로 1:1로 얹는다.
 * 공간 클러스터는 원래 "끈을 따라간 순서" 정보가 없어(스컬핑 메시라 위상 정보가 없음) 진짜
 * 냉색→온색 흐름을 재현할 수는 없지만, 이 정렬로 적어도 하나의 축을 따라 부드럽게 색이 바뀌는
 * 느낌은 최선으로 재현한다.
 */
object KnotClusterColorMapping {

    /**
     * @param dailyColorHexes 그 달의 실 색 hex 리스트(하루당 하나, 정렬 불필요, 중복 허용).
     * @param clusterCount 매듭 타입의 클러스터 개수([KnotClusterCenters.CLUSTER_COUNT]).
     * @return 클러스터 순서(중심 좌표 오름차순)에 1:1로 대응하는 hex 색 리스트. 입력이 비어 있으면
     *   빈 리스트(호출부는 이 경우 3순위 tryTintBaseColor 등으로 폴백해야 한다).
     */
    fun mapMonthlyColorsToClusters(dailyColorHexes: List<String>, clusterCount: Int): List<String> {
        if (clusterCount <= 0) return emptyList()
        val validHexes = dailyColorHexes.filter { it.isNotBlank() }
        if (validHexes.isEmpty()) return emptyList()

        val counts: Map<String, Int> = validHexes.groupingBy { it.lowercase() }.eachCount()
        // 핵심 불변식: 채울 색의 후보 풀은 "그 달 실제로 획득한 색"으로 못 박는다. 이후 모든 단계
        // (몫 배정·최대잉여법·펼치기)는 이 pool 밖의 값을 절대 만들어내지 않는다 — 새 색을 합성하는
        // 연산(보간·평균 등)이 전혀 없고, 이 리스트에서 원소를 "선택"하고 "반복"만 하기 때문이다.
        // 냉색→온색 스펙트럼 순으로 정렬해 [KnotClusterCenters]의 클러스터 순서에 그대로 얹는다.
        val acquiredHexesBySpectrum: List<String> = counts.keys.sortedBy { ThreadColors.spectrumIndexOf(it) }
        val total = validHexes.size

        // 1) 비율의 정수부(floor)만큼 먼저 배정.
        val exactShares = acquiredHexesBySpectrum.associateWith { hex ->
            counts.getValue(hex).toDouble() * clusterCount / total
        }
        val bands = acquiredHexesBySpectrum.associateWith { hex -> exactShares.getValue(hex).toInt() }.toMutableMap()
        var assigned = bands.values.sum()

        // 2) 최대잉여법: 소수부 큰 색 우선, 동률이면 스펙트럼 인덱스 낮은(냉색) 색 우선.
        //    (색 종류가 clusterCount보다 많은 달엔 이 단계에서 자연히 밴드 0개인 색이 생긴다 — 그
        //    색은 이번엔 매듭에 안 나타난다. 손실은 감수하지만, 나타나는 색은 여전히 전부 후보
        //    풀 안이다.)
        val bySpectrumRank = acquiredHexesBySpectrum.withIndex().associate { (i, hex) -> hex to i }
        val remainderOrder = acquiredHexesBySpectrum
            .map { hex -> hex to (exactShares.getValue(hex) - bands.getValue(hex)) }
            .sortedWith(
                compareByDescending<Pair<String, Double>> { it.second }
                    .thenBy { bySpectrumRank.getValue(it.first) }
            )
        var i = 0
        while (assigned < clusterCount && i < remainderOrder.size) {
            val hex = remainderOrder[i].first
            bands[hex] = bands.getValue(hex) + 1
            assigned++
            i++
        }
        // clusterCount가 그 달에 등장한 색 종류 수보다 훨씬 많은 극단적인 경우(예: 한 색만 있는 달 →
        // 7개 구역 전부 그 한 색) 한 바퀴로 다 못 채우면 스펙트럼 순으로 계속 돌며 채운다. 여전히
        // acquiredHexesBySpectrum 안에서만 순회한다.
        while (assigned < clusterCount) {
            for (hex in acquiredHexesBySpectrum) {
                if (assigned >= clusterCount) break
                bands[hex] = bands.getValue(hex) + 1
                assigned++
            }
        }

        // 3) 냉색→온색 순으로 펼쳐 클러스터 순서에 그대로 얹는다.
        val expanded = mutableListOf<String>()
        for (hex in acquiredHexesBySpectrum) {
            repeat(bands.getValue(hex)) { expanded.add(hex) }
        }
        val result = expanded.take(clusterCount)

        // 방어적 후검증: 위 로직이 앞으로 바뀌어도 "미획득 색은 절대 안 나온다"는 원칙이 깨지면
        // 여기서 바로 터진다(조용히 잘못된 색을 그리는 것보다 낫다).
        check(result.all { it in acquiredHexesBySpectrum }) {
            "clusterColors에 그 달 미획득 색이 섞였다: $result vs $acquiredHexesBySpectrum"
        }
        return result
    }
}
