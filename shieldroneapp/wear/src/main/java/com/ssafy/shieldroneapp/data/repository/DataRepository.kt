package com.ssafy.shieldroneapp.data.repository

import com.ssafy.shieldroneapp.data.model.HeartRateData

interface DataRepository {
    suspend fun hasHeartRateCapability(): Boolean
    fun getHeartRateMeasures(): kotlinx.coroutines.flow.Flow<HeartRateData>
}
