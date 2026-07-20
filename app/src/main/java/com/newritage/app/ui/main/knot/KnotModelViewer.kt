package com.newritage.app.ui.main.knot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.utils.Manipulator
import io.github.sceneview.Scene
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.material.setColor
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.model.renderableEntities
import io.github.sceneview.model.renderableManager
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberNodes
import kotlin.math.sqrt

// glTF 표준 PBR 머티리얼의 색상 파라미터 이름 후보. Filament의 gltfio 임포터가 생성하는
// 머티리얼에 존재하는 이름만 실제로 적용되고, 나머지는 조용히 무시된다.
private val BASE_COLOR_PARAMETER_CANDIDATES = listOf("baseColorFactor", "baseColor")

/** 세션에 저장된 hex 문자열(예: threadColor)을 Compose Color로 안전하게 변환한다. */
fun knotTintColorOrNull(hex: String): Color? {
    if (hex.isBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * 매듭별 3D 뷰어 초기 정렬 보정값.
 * @param rotationY 기본 회전(x=-90) 위에 더할 Y축 회전(도). 원본이 뒤를 보고 있는 매듭은 180을 줘 정면을 맞춘다.
 * @param scaleMul  scaleToUnits 기준 크기 배율. 1.0이 기본이고 살짝 키우고 싶은 매듭만 올린다.
 * 값들은 실기기에서 눈으로 보며 조정한다(3D SurfaceView는 캡처가 안 돼 자동 검증이 불가).
 */
private data class KnotViewAdjust(val rotationY: Float = 0f, val scaleMul: Float = 1f)

private fun knotViewAdjustFor(assetPath: String): KnotViewAdjust {
    return when (assetPath.substringAfterLast('/').removeSuffix(".glb")) {
        // 정면이 뒤를 향해 있어 180도 돌리고 살짝 키움
        "garakji" -> KnotViewAdjust(rotationY = 180f, scaleMul = 1.15f)
        "gukhwa" -> KnotViewAdjust(rotationY = 180f, scaleMul = 1.15f)
        "samjeongja" -> KnotViewAdjust(rotationY = 180f, scaleMul = 1.15f)
        "angyeong" -> KnotViewAdjust(rotationY = 180f, scaleMul = 1.15f)
        // 위치는 좋고 크기만 10%가량 키움
        "maehwa" -> KnotViewAdjust(scaleMul = 1.1f)
        "saengjjok" -> KnotViewAdjust(scaleMul = 1.1f)
        // dorae, nabi, byeongari, gajibangseok: 기본값
        else -> KnotViewAdjust()
    }
}

private fun MaterialInstance.tryTintBaseColor(color: Color) {
    for (paramName in BASE_COLOR_PARAMETER_CANDIDATES) {
        try {
            setColor(paramName, color)
            return
        } catch (_: IllegalArgumentException) {
            // 이 머티리얼에 없는 파라미터 이름이면 다음 후보를 시도한다.
        }
    }
}

/**
 * 클러스터 경계를 부드럽게 섞는 전이 폭을, "그 매듭 타입의 클러스터 간 평균 최근접 간격"에 이
 * 비율을 곱해서 정한다. 매듭마다 object-space 스케일이 완전히 다르므로(예: 병아리매듭 bbox
 * 대각선 ~580 vs 도래매듭 ~0.21) 절대값 상수 하나를 모든 매듭에 그대로 쓰면 어떤 매듭에서는
 * 안 보이고 어떤 매듭에서는 전체가 뭉개진다. 이 비율만 조절하면 모든 매듭에서 일관되게 폭이
 * 넓어지거나 좁아진다 — 0에 가까울수록 하드 엣지, 너무 크면(1 근처 이상) 인접 클러스터끼리 서로
 * 넓게 섞여 7색이 다 탁해지므로 "경계 근처로만 국한"하려면 1보다 확실히 작게 유지해야 한다.
 */
private const val CLUSTER_BLEND_FRACTION = 0.3f

/** [centers] 각 점에서 가장 가까운 다른 점까지의 거리를 평균한다. 점이 1개 이하면 0(블렌드 없음). */
private fun averageNearestNeighborSpacing(centers: List<Position>): Float {
    if (centers.size < 2) return 0f
    var sum = 0f
    for (i in centers.indices) {
        var minDist = Float.MAX_VALUE
        for (j in centers.indices) {
            if (i == j) continue
            val dx = centers[i].x - centers[j].x
            val dy = centers[i].y - centers[j].y
            val dz = centers[i].z - centers[j].z
            val dist = sqrt(dx * dx + dy * dy + dz * dz)
            if (dist < minDist) minDist = dist
        }
        sum += minDist
    }
    return sum / centers.size
}

/**
 * knot_cluster.mat의 clusterCenters[7]/clusterColors[7]/clusterCount/clusterBlendWidth 유니폼을
 * 채운다. centers·colorHexes는 [KnotClusterCenters]/[KnotClusterColorMapping]이 이미 클러스터
 * 순서(중심 좌표 오름차순)로 1:1 대응하게 만들어 넘겨준다고 가정한다. 두 리스트 길이가 다르면
 * 짧은 쪽에 맞춘다.
 */
private fun MaterialInstance.setClusterUniforms(centers: List<Position>, colorHexes: List<String>) {
    val count = minOf(centers.size, colorHexes.size, KnotClusterCenters.CLUSTER_COUNT)
    val centerFloats = FloatArray(KnotClusterCenters.CLUSTER_COUNT * 3)
    val colorFloats = FloatArray(KnotClusterCenters.CLUSTER_COUNT * 3)
    for (i in 0 until count) {
        val c = centers[i]
        centerFloats[i * 3] = c.x
        centerFloats[i * 3 + 1] = c.y
        centerFloats[i * 3 + 2] = c.z

        val argb = android.graphics.Color.parseColor(colorHexes[i])
        colorFloats[i * 3] = android.graphics.Color.red(argb) / 255f
        colorFloats[i * 3 + 1] = android.graphics.Color.green(argb) / 255f
        colorFloats[i * 3 + 2] = android.graphics.Color.blue(argb) / 255f
    }
    setParameter(
        "clusterCenters", MaterialInstance.FloatElement.FLOAT3,
        centerFloats, 0, KnotClusterCenters.CLUSTER_COUNT
    )
    setParameter(
        "clusterColors", MaterialInstance.FloatElement.FLOAT3,
        colorFloats, 0, KnotClusterCenters.CLUSTER_COUNT
    )
    setParameter("clusterCount", count)
    setParameter("clusterBlendWidth", averageNearestNeighborSpacing(centers.take(count)) * CLUSTER_BLEND_FRACTION)
}

/** [modelInstance]의 모든 프리미티브 머티리얼을 [materialInstance] 하나로 교체한다. */
private fun applyMaterialToAllPrimitives(
    modelInstance: com.google.android.filament.gltfio.FilamentInstance,
    materialInstance: MaterialInstance
) {
    val renderableManager = modelInstance.renderableManager
    modelInstance.renderableEntities.forEach { entity ->
        val renderable = renderableManager.getInstance(entity)
        val primitiveCount = renderableManager.getPrimitiveCount(renderable)
        for (primitiveIndex in 0 until primitiveCount) {
            renderableManager.setMaterialInstanceAt(renderable, primitiveIndex, materialInstance)
        }
    }
}

/**
 * assets/[glbAssetPath] 위치의 GLB 매듭 모델을 보여준다.
 *
 * @param interactive false면 회전/줌/이동 제스처를 전부 잠가 캘린더 그리드용 "고정된 썸네일"로 쓰고,
 *   true면 회전(orbit)만 가능하고 이동(pan)·줌은 잠근 상세보기 뷰어로 동작한다.
 * @param tintColor null이면 모델에 저장된 원래 색을 그대로 쓰고, 값을 주면 모든 머티리얼의 기본색을
 *   이 색으로 덮어쓴다. [monthlyThreadColorHexes]가 비어 있을 때만 쓰이는 3순위(단색) 폴백이다.
 * @param monthlyThreadColorHexes 그 달의 실 색 hex 리스트(하루당 하나, 순서 무관). 비어 있지 않으면
 *   2순위(공간 클러스터링 + 좌표 기반 커스텀 셰이더)로 매듭을 여러 색 덩어리로 칠한다 — [tintColor]보다
 *   우선한다. 비어 있으면(그 달에 실 데이터가 없으면) [tintColor] 단색 틴트로 폴백한다.
 * @param engine/modelLoader 기본값은 [KnotEngineHolder]가 앱 전체에서 공유하는 단일 엔진이다.
 *   화면(그리드 썸네일/상세보기)마다 엔진을 새로 만들고 파괴하면 한 화면이 사라지며 엔진을 파괴하는
 *   시점과 다른 화면이 새 엔진을 만드는 시점이 겹쳐 Filament 네이티브 크래시가 나기 때문에, 항상 이
 *   공유 엔진을 재사용한다.
 */
@Composable
fun KnotModelViewer(
    glbAssetPath: String,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    tintColor: Color? = null,
    monthlyThreadColorHexes: List<String> = emptyList(),
    backgroundColor: Color = Color(0xFFF4F5EF),
    modelRotation: Rotation = Rotation(x = -90f, y = 0f, z = 0f),
    cameraDistance: Float = 3f,
    engine: Engine = KnotEngineHolder.engine(),
    modelLoader: ModelLoader = KnotEngineHolder.modelLoader(LocalContext.current)
) {
    val context = LocalContext.current
    val childNodes = rememberNodes()
    // 2순위 클러스터 MaterialInstance는 Filament 네이티브 리소스라 GC로 안 치워진다. 매번 새로
    // 만들 때 이전 것을 직접 destroy해야 하므로, Compose 재구성과 무관하게 참조를 들고 있을 배열
    // 하나짜리 홀더(굳이 State로 만들어 재구성을 유발할 필요가 없다)를 둔다.
    val activeClusterMaterialInstance = remember { arrayOfNulls<MaterialInstance>(1) }
    DisposableEffect(engine) {
        onDispose {
            activeClusterMaterialInstance[0]?.let { engine.destroyMaterialInstance(it) }
            activeClusterMaterialInstance[0] = null
        }
    }
    val cameraManipulator = remember(interactive, cameraDistance) {
        CameraGestureDetector.DefaultCameraManipulator(
            Manipulator.Builder()
                .targetPosition(0f, 0f, 0f)
                .orbitHomePosition(0f, 0f, cameraDistance)
                // x(가로 드래그)만 허용해 세로축 회전 없이 가로축 360도 회전만 가능하게 한다.
                .orbitSpeed(if (interactive) 0.005f else 0f, 0f)
                .zoomSpeed(0f) // 확대/축소 제스처는 항상 잠근다 (썸네일·상세보기 공통)
                .build(Manipulator.Mode.ORBIT)
        )
    }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(glbAssetPath, tintColor, monthlyThreadColorHexes, modelRotation, modelLoader) {
        // 날짜를 넘길 때마다 이전 매듭 모델을 지우지 않으면 원점에 계속 겹쳐 쌓인다.
        childNodes.toList().forEach { it.destroy() }
        childNodes.clear()
        // 이번 실행에서 2순위를 다시 쓰든 안 쓰든, 지난번에 만든 클러스터 MaterialInstance부터
        // 먼저 정리한다 — 이번엔 실 데이터가 없어져 3순위로 폴백하더라도 새는 일이 없게.
        activeClusterMaterialInstance[0]?.let { engine.destroyMaterialInstance(it) }
        activeClusterMaterialInstance[0] = null

        val modelInstance = modelLoader.loadModelInstance(glbAssetPath)
        if (modelInstance != null) {
            val clusterColors = KnotClusterColorMapping.mapMonthlyColorsToClusters(
                monthlyThreadColorHexes,
                KnotClusterCenters.CLUSTER_COUNT
            )
            if (clusterColors.isNotEmpty()) {
                // 2순위: 공간 클러스터링 + 좌표 기반 커스텀 셰이더로 여러 색 덩어리를 칠한다.
                // 지금은 클러스터 경계를 하드 엣지로 그대로 둔다(knot_cluster.mat의 nearest-center
                // 룩업). 나중에 경계가 딱딱해 보이면 여러 중심까지의 거리를 가중치로 부드럽게 섞는
                // 옵션을 셰이더에 추가할 수 있다 — 지금은 "확실히 동작하는 것"을 우선한다.
                val centers = KnotClusterCenters.forAssetPath(glbAssetPath)
                val clusterMaterialInstance = KnotEngineHolder.clusterMaterialInstance(context)
                clusterMaterialInstance.setClusterUniforms(centers, clusterColors)
                applyMaterialToAllPrimitives(modelInstance, clusterMaterialInstance)
                activeClusterMaterialInstance[0] = clusterMaterialInstance
            } else if (tintColor != null) {
                // 3순위 폴백: 그 달 실 데이터가 없을 때만(threadColor 세션이 아직 없는 달) 단색 틴트.
                modelInstance.materialInstances.forEach { materialInstance ->
                    materialInstance.tryTintBaseColor(tintColor)
                }
            }
            // 매듭마다 원본 모델의 정면 방향과 적정 크기가 조금씩 달라, 파일명 기준으로 개별
            // 보정값(추가 Y축 회전 + 크기 배율)을 적용해 첫 렌더부터 정렬을 맞춘다.
            // (모든 GLB는 지오메트리 중심이 원점에 오도록 정규화돼 있어, scaleToUnits+centerOrigin
            //  조합만으로 확대해도 중앙 정렬이 유지된다.)
            val adjust = knotViewAdjustFor(glbAssetPath)
            childNodes.add(
                ModelNode(
                    modelInstance = modelInstance,
                    autoAnimate = false,
                    scaleToUnits = adjust.scaleMul,
                    centerOrigin = Position(0f, 0f, 0f)
                ).apply {
                    rotation = Rotation(
                        x = modelRotation.x,
                        y = modelRotation.y + adjust.rotationY,
                        z = modelRotation.z
                    )
                }
            )
        }
        isLoading = false
    }

    Box(modifier = modifier.background(backgroundColor)) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            isOpaque = false,
            cameraManipulator = cameraManipulator,
            childNodes = childNodes,
            onViewCreated = {
                // 이동(pan) 제스처는 CameraManipulator의 speed로 막을 수 없어 제스처 감지기 단에서 잠근다.
                cameraGestureDetector?.isPanEnabled = false
                // SceneView는 사용자가 한 번이라도 터치하기 전까지 카메라에 매니퓰레이터 값을 반영하지
                // 않아, 터치 전에는 Filament의 미보정 기본 카메라로 크게 보이다가 터치하는 순간 올바른
                // cameraDistance 크기로 줄어드는 것처럼 보인다. 여기서 미리 한 번 반영해 첫 프레임부터
                // 최종 크기로 보이게 한다.
                cameraManipulator?.let { manipulator ->
                    manipulator.update(0f)
                    cameraNode.transform = manipulator.getTransform()
                }
            }
        )
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
