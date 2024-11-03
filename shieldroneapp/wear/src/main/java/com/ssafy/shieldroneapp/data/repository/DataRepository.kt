package com.ssafy.shieldroneapp.data.repository

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.utils.await

class DataRepository(private val context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)

    suspend fun sendHeartRateData(heartRateData: HeartRateData) {
        try {
            val putDataMapRequest = PutDataMapRequest.create("/heart_rate").apply {
                dataMap.putDouble("bpm", heartRateData.bpm)
                dataMap.putLong("timestamp", heartRateData.timestamp)
                dataMap.putString("availability", heartRateData.availability.name)
            }

            val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataRequest).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}