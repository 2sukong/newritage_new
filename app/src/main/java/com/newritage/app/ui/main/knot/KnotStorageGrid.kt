package com.newritage.app.ui.main.knot

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newritage.app.R
import com.newritage.app.data.KnotType

/**
 * 매듭 보관함 한 칸(연도 그리드의 한 달)에 대응하는 데이터. 매듭은 월 단위로 지급되므로,
 * 그 달에 아직 매듭을 얻지 못했으면 칸 자체가 없다.
 */
data class KnotGridEntry(
    /** 상세 시트를 열 때 넘기는 "yyyy-MM" 키 */
    val key: String,
    /** 칸 아래 표시할 라벨(예: "2월") */
    val label: String,
    val knotType: KnotType,
    val tintColorHex: String,
    val isNew: Boolean
)

private val NEW_ACCENT_COLOR = Color(0xFFD3ED15)
private val DAY_LABEL_COLOR = Color(0xFF5A6B5A)
// 실 보관함의 GradientBorderDrawable과 동일한 그라데이션(상단 D6D6D6 → 하단 BABCBA).
private val GRAY_BORDER_GRADIENT = Brush.verticalGradient(
    colors = listOf(Color(0xFFD6D6D6), Color(0xFFBABCBA))
)
private val FRAME_SHAPE = RoundedCornerShape(8.dp)
private val FRAME_BORDER_WIDTH = 1.dp
// NEW 상태일 때 회색 테두리 프레임 위에 덧씌우는 별도의 두꺼운 프레임.
private val NEW_FRAME_SHAPE = RoundedCornerShape(12.dp)
private val NEW_FRAME_BORDER_WIDTH = 3.dp

/**
 * 매듭을 얻은 날짜만큼만 칸이 늘어나는 그리드. 각 칸은 회전/줌 없이 고정된 각도로만 보이므로,
 * 매번 라이브 3D를 렌더링하는 대신 흰색 재질로 미리 렌더링해 둔 정적 이미지(KnotType.thumbnailRes)에
 * 세션 실 색상을 곱연산(Modulate) 틴트해서 보여준다. 한 달치 칸이 전부 라이브 Filament 렌더러였을 때는
 * 최대 31개의 SurfaceView/렌더 루프가 동시에 떠 렉이 심했는데, 정적 이미지로 바꾸면 그리드에는 3D
 * 렌더러가 전혀 없어져 그 렉이 사라진다(상세보기의 회전 가능한 3D 뷰는 그대로 유지).
 */
@Composable
fun KnotStorageGrid(
    entries: List<KnotGridEntry>,
    modifier: Modifier = Modifier,
    onEntryClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries, key = { it.key }) { entry ->
            KnotGridCell(
                entry = entry,
                onClick = { onEntryClick(entry.key) }
            )
        }
    }
}

@Composable
private fun KnotGridCell(
    entry: KnotGridEntry,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // GONE 대신 alpha(0f): NEW 배지 유무와 상관없이 모든 칸이 같은 세로 구조를
        // 가져야 프레임이 줄마다 같은 높이에서 정렬된다(실 보관함의 INVISIBLE 처리와 동일).
        Text(
            text = stringResource(R.string.badge_new),
            color = NEW_ACCENT_COLOR,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 2.dp)
                .alpha(if (entry.isNew) 1f else 0f)
        )

        // NEW 배지 유무와 상관없이 바깥 프레임 자리는 항상 같은 크기로 예약해 둔다.
        // NEW가 아닐 때는 이 자리의 테두리 색을 투명하게만 둬서, 상태가 바뀌어도
        // 칸/레이아웃이 아래로 밀리지 않게 한다.
        Box(
            modifier = Modifier
                .width(64.dp + NEW_FRAME_BORDER_WIDTH * 2)
                .height(76.dp + NEW_FRAME_BORDER_WIDTH * 2)
                .border(
                    width = NEW_FRAME_BORDER_WIDTH,
                    color = if (entry.isNew) NEW_ACCENT_COLOR else Color.Transparent,
                    shape = NEW_FRAME_SHAPE
                )
                .padding(NEW_FRAME_BORDER_WIDTH)
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = FRAME_BORDER_WIDTH,
                        brush = GRAY_BORDER_GRADIENT,
                        shape = FRAME_SHAPE
                    )
            ) {
                Image(
                    painter = painterResource(entry.knotType.thumbnailRes),
                    contentDescription = null,
                    colorFilter = knotTintColorOrNull(entry.tintColorHex)
                        ?.let { ColorFilter.tint(it, BlendMode.Modulate) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            }
        }

        Text(
            text = entry.label,
            fontSize = 11.sp,
            color = DAY_LABEL_COLOR,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
