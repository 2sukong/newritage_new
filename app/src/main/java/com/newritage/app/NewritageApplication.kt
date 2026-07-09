package com.newritage.app

import android.app.Application
import com.newritage.app.network.GeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 앱 시작 시 Gemini API 연결을 확인하기 위한 임시 테스트 호출을 수행한다. */
class NewritageApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            GeminiService.sendTestPrompt()
        }
    }
}
