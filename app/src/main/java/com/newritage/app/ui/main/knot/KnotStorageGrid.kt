package com.newritage.app.ui.main.knot

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newritage.app.R
import com.newritage.app.data.KnotType

/** 매듭 보관함 한 칸에 대응하는 데이터. 매듭을 얻은 날짜에만 만들어지므로, 얻지 못한 날은 칸 자체가 없다. */
data class KnotGridEntry(
    val date: String,
    val day: Int,
    val knotType: KnotType,
    val tintColorHex: String,
    val isNew: Boolean
)

private val CELL_BORDER_COLOR = Color(0xFFE0E0D8)
private val NEW_ACCENT_COLOR = Color(0xFFB5CC3E)
private val DAY_LABEL_COLOR = Color(0xFF5A6B5A)

/**
 * 매듭을 얻은 날짜만큼만 칸이 늘어나는 그리드. 각 칸은 회전/줌 없이 고정된 각도로만 보이는
 * 작은 3D 썸네일이다(자세히 보려면 칸을 탭해 상세 화면으로 이동). 여러 칸이 하나의 Filament
 * 엔진/모델 로더를 공유해 다수의 3D 썸네일을 동시에 띄우는 비용을 줄인다.
 */
@Composable
fun KnotStorageGrid(
    entries: List<KnotGridEntry>,
    modifier: Modifier = Modifier,
    onEntryClick: (String) -> Unit
) {
    val context = LocalContext.current
    val engine = KnotEngineHolder.engine()
    val modelLoader = KnotEngineHolder.modelLoader(context)

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries, key = { it.date }) { entry ->
            KnotGridCell(
                entry = entry,
                engine = engine,
                modelLoader = modelLoader,
                onClick = { onEntryClick(entry.date) }
            )
        }
    }
}

@Composable
private fun KnotGridCell(
    entry: KnotGridEntry,
    engine: com.google.android.filament.Engine,
    modelLoader: io.github.sceneview.loaders.ModelLoader,
    onClick: () -> Unit
) {
    val cellShape = RoundedCornerShape(10.dp)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (entry.isNew) {
            Text(
                text = stringResource(R.string.badge_new),
                color = NEW_ACCENT_COLOR,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(64.dp)
                .height(76.dp)
                .clip(cellShape)
                .border(
                    if (entry.isNew) 2.dp else 1.dp,
                    if (entry.isNew) NEW_ACCENT_COLOR else CELL_BORDER_COLOR,
                    cellShape
                )
        ) {
            KnotModelViewer(
                glbAssetPath = entry.knotType.assetPath,
                interactive = false,
                tintColor = knotTintColorOrNull(entry.tintColorHex),
                engine = engine,
                modelLoader = modelLoader,
                modifier = Modifier.fillMaxSize()
            )
            // 3D 썸네일의 SurfaceView가 터치를 먼저 가로채므로, 탭을 확실히 받기 위해
            // 투명한 클릭 레이어를 그 위에 겹쳐 둔다.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick)
            )
        }

        Text(
            text = stringResource(R.string.day_number_format, entry.day),
            fontSize = 11.sp,
            color = DAY_LABEL_COLOR,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
