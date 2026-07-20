package com.newritage.app.ui.main.knot

import android.content.Context
import com.google.android.filament.Engine
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import io.github.sceneview.SceneView
import io.github.sceneview.loaders.ModelLoader
import java.nio.ByteBuffer

/**
 * 매듭 3D 뷰어(그리드 썸네일 + 상세보기)가 공유하는 단일 Filament 엔진.
 *
 * 화면(Fragment)이 바뀔 때마다 Compose의 rememberEngine()/rememberModelLoader()로 엔진을 새로
 * 만들고 파괴하면, 그리드 화면이 사라지며 이전 엔진을 파괴하는 시점과 상세보기 화면이 새 엔진을
 * 만드는 시점이 겹쳐 Filament 내부에서 네이티브 크래시(PostconditionPanic, FEngine::loop)가 났다.
 * 앱 전체에서 엔진을 하나만 만들어 계속 재사용하면 이 파괴/생성 경합이 아예 생기지 않는다.
 */
object KnotEngineHolder {
    @Volatile private var engine: Engine? = null
    @Volatile private var modelLoader: ModelLoader? = null
    @Volatile private var clusterMaterial: Material? = null

    fun engine(): Engine =
        engine ?: synchronized(this) {
            engine ?: SceneView.createEngine(SceneView.createEglContext()).also { engine = it }
        }

    fun modelLoader(context: Context): ModelLoader =
        modelLoader ?: synchronized(this) {
            modelLoader ?: SceneView.createModelLoader(engine(), context.applicationContext).also {
                modelLoader = it
            }
        }

    /**
     * 2순위(공간 클러스터링 + 좌표 기반 커스텀 셰이더) 머티리얼. assets/materials/knot_cluster.filamat
     * (matc 1.56.0으로 컴파일, Filament 1.56.0과 버전 일치)를 로드한다. 컴파일된 [Material]은 엔진처럼
     * 앱 전체에서 1회만 만들어 재사용하고([[reference-local-gradle-build]]와 같은 이유로 네이티브
     * 리소스는 가볍게 만들고 버리지 않는다), 매듭마다/매달마다 달라지는 값(클러스터 중심·색)은 이
     * Material로부터 매번 새로 뽑아낸 [MaterialInstance]에 유니폼 파라미터로 얹는다.
     * MaterialInstance는 호출부(KnotModelViewer)가 수명을 책임지고 다 쓰면 반드시
     * engine().destroyMaterialInstance()로 정리해야 한다 — Filament 네이티브 리소스라 GC로 안 치워진다.
     */
    fun clusterMaterialInstance(context: Context): MaterialInstance {
        val material = clusterMaterial ?: synchronized(this) {
            clusterMaterial ?: run {
                val bytes = context.applicationContext.assets
                    .open("materials/knot_cluster.filamat")
                    .use { it.readBytes() }
                val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                    put(bytes)
                    rewind()
                }
                Material.Builder().payload(buffer, buffer.remaining()).build(engine())
            }.also { clusterMaterial = it }
        }
        return material.createInstance()
    }
}
