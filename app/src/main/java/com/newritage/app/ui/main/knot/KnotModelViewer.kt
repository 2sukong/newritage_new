package com.newritage.app.ui.main.knot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
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
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberNodes

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
 * assets/[glbAssetPath] 위치의 GLB 매듭 모델을 보여준다.
 *
 * @param interactive false면 회전/줌/이동 제스처를 전부 잠가 캘린더 그리드용 "고정된 썸네일"로 쓰고,
 *   true면 회전(orbit)만 가능하고 이동(pan)·줌은 잠근 상세보기 뷰어로 동작한다.
 * @param tintColor null이면 모델에 저장된 원래 색을 그대로 쓰고, 값을 주면 모든 머티리얼의 기본색을
 *   이 색으로 덮어쓴다(이달의 색상 알고리즘이 정해지기 전까지 임시로 세션의 실 색상을 그대로 사용한다).
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
    backgroundColor: Color = Color(0xFFF4F5EF),
    modelRotation: Rotation = Rotation(x = -90f, y = 0f, z = 0f),
    cameraDistance: Float = 3f,
    engine: Engine = KnotEngineHolder.engine(),
    modelLoader: ModelLoader = KnotEngineHolder.modelLoader(LocalContext.current)
) {
    val childNodes = rememberNodes()
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

    LaunchedEffect(glbAssetPath, tintColor, modelRotation, modelLoader) {
        // 날짜를 넘길 때마다 이전 매듭 모델을 지우지 않으면 원점에 계속 겹쳐 쌓인다.
        childNodes.toList().forEach { it.destroy() }
        childNodes.clear()
        val modelInstance = modelLoader.loadModelInstance(glbAssetPath)
        if (modelInstance != null) {
            if (tintColor != null) {
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
