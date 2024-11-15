package com.shieldrone.station.data

data class TrackingDataDiff(
    val oldData: TrackingData,
    val newData: TrackingData,
    val delayInMillis: Long = 200 // 지연 시간 (밀리초)
) {
    // 시간 차이 (초 단위)
    val deltaTime: Double = (newData.receivedTime - oldData.receivedTime) / 1000.0

    // 정규화된 오프셋의 변화량
    val deltaNormalizedOffsetX: Double = newData.normalizedOffsetX - oldData.normalizedOffsetX
    val deltaNormalizedOffsetY: Double = newData.normalizedOffsetY - oldData.normalizedOffsetY

    // 오차 변화율 (초당 변화율)
    val errorRateX: Double = if (deltaTime > 0) deltaNormalizedOffsetX / deltaTime else 0.0
    val errorRateY: Double = if (deltaTime > 0) deltaNormalizedOffsetY / deltaTime else 0.0

    // 지연 시간 (초 단위)
    private val deltaTDelay: Double = delayInMillis / 1000.0

    // 미래 오차 예측
    val futureErrorX: Double = newData.normalizedOffsetX + errorRateX * deltaTDelay
    val futureErrorY: Double = newData.normalizedOffsetY + errorRateY * deltaTDelay
}
