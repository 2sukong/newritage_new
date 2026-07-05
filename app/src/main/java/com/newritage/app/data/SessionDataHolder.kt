package com.newritage.app.data

// MeasurementActivity에서 SessionCompleteActivity로 원시 데이터를 넘기기 위한 임시 저장소
// (Intent로 넘기기엔 데이터가 크므로 메모리에 잠깐 담아두는 방식)
object SessionDataHolder {
    var sensorReadings: List<SensorReading> = emptyList()
    var vibrationCount: Int = 0

    fun clear() {
        sensorReadings = emptyList()
        vibrationCount = 0
    }
}
