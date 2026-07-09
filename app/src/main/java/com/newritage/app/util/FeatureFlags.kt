package com.newritage.app.util

object FeatureFlags {
    /** true면 기기(BLE)가 연결되어 있지 않을 때 초기/일반 측정 시작을 막는다.
     *  false로 바꾸면 연결 여부와 상관없이 바로 측정을 시작할 수 있다. */
    const val REQUIRE_BLE_CONNECTION_TO_START = false
}
