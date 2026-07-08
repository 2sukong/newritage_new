package com.newritage.app.ui.main.knot

import android.content.Context
import com.google.android.filament.Engine
import io.github.sceneview.SceneView
import io.github.sceneview.loaders.ModelLoader

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
}
