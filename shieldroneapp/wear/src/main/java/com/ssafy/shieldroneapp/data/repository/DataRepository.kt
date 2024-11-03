package com.ssafy.shieldroneapp.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.utils.await

class DataRepository(private val context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)

    companion object {
        private const val THRESHOLD_BPM = 70.0
        private const val TAG = "DataRepository"
    }

    suspend fun sendHeartRateData(heartRateData: HeartRateData) {
        try {
            if (heartRateData.bpm >= THRESHOLD_BPM) {
                val putDataMapRequest = PutDataMapRequest.create("/heart_rate").apply {
                    dataMap.putDouble("bpm", heartRateData.bpm)
                    dataMap.putLong("timestamp", heartRateData.timestamp)
                    dataMap.putString("availability", heartRateData.availability.name)
                }

                val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()
                dataClient.putDataItem(putDataRequest).await()
                Log.d(TAG, "Heart rate data sent: ${heartRateData.bpm} BPM")
            } else {
                Log.d(TAG, "Heart rate below threshold (${heartRateData.bpm} BPM), not sending")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending heart rate data", e)
            e.printStackTrace()
        }
    }
}