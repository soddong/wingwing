package com.ssafy.shieldroneapp.services.connection

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.data.repository.SensorDataRepository
import com.ssafy.shieldroneapp.services.base.BaseMobileService
import kotlinx.coroutines.launch
import javax.inject.Inject

class WearableDataListenerService : BaseMobileService() {

    @Inject
    lateinit var sensorDataRepository: SensorDataRepository

    companion object {
        private const val TAG = "모바일: 웨어러블 기기 리스너"
        private const val PATH_HEART_RATE = "/sendPulseFlag"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                when (event.dataItem.uri.path) {
                    PATH_HEART_RATE -> processHeartRateData(event)
                }
            }
        }
    }

    private fun processHeartRateData(event: DataEvent) {
        try {
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val pulseFlag = dataMap.getBoolean("pulseFlag")
            val timestamp = dataMap.getLong("timestamp")

            Log.d(TAG, "심박수 수신: $pulseFlag, 기록된 시간: $timestamp")

            serviceScope.launch {
                sensorDataRepository.processHeartRateData(
                    HeartRateData(
                        pulseFlag = pulseFlag,
                        timestamp = timestamp
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 처리중 에러 발생", e)
        }
    }
}