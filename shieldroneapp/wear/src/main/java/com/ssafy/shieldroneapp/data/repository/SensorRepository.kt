package com.ssafy.shieldroneapp.data.repository

import android.content.Context
import android.location.LocationManager
import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.SampleDataPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking

class SensorRepository(context: Context) {
    private val healthServicesClient = HealthServices.getClient(context)
    private val measureClient = healthServicesClient.measureClient
    private val exerciseClient = healthServicesClient.exerciseClient
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // 속도 capability 상태를 관찰할 수 있는 Flow
    private val _speedCapabilityFlow = MutableStateFlow<Boolean>(false)
    val speedCapabilityFlow = _speedCapabilityFlow.asStateFlow()

    suspend fun hasHeartRateCapability(): Boolean {
        val capabilities = measureClient.getCapabilitiesAsync().await()
        return (DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure)
    }

    suspend fun checkSpeedCapability() {
        val capabilities = measureClient.getCapabilitiesAsync().await()
        Log.d("SensorRepository", "All supported capabilities: ${capabilities.supportedDataTypesMeasure}")
        val hasSpeed = DataType.SPEED in capabilities.supportedDataTypesMeasure
        _speedCapabilityFlow.value = hasSpeed
        Log.d("SensorRepository", "Speed capability: $hasSpeed")

        val supportedDataTypes = capabilities.supportedDataTypesMeasure

        Log.d("SensorRepository", "Supported data types:")
        supportedDataTypes.forEach { dataType ->
            Log.d("가능 타입 확인", "DataType: ${dataType.name}")
        }
    }



    fun heartRateMeasureFlow() = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {
                if (availability is DataTypeAvailability) {
                    trySendBlocking(MeasureMessage.MeasureAvailability(availability))
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                val heartRateBpm = data.getData(DataType.HEART_RATE_BPM)
                trySendBlocking(MeasureMessage.MeasureData(heartRateBpm))
            }
        }

        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)

        awaitClose {
            runBlocking {
                measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback)
                    .await()
            }
        }
    }

    suspend fun hasSpeedCapability(): Boolean {
        val capabilities = measureClient.getCapabilitiesAsync().await()
        val hasSpeed = DataType.SPEED in capabilities.supportedDataTypesMeasure

        _speedCapabilityFlow.value = hasSpeed

        Log.d("SensorRepository", "Speed capability check: $hasSpeed")
        return hasSpeed
    }

    fun speedMeasureFlow() = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {
                Log.d("SensorRepository", "Speed availability changed: $availability")
                if (availability is DataTypeAvailability) {
                    trySendBlocking(MeasureMessage.MeasureAvailability(availability))
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                Log.d("SensorRepository", "Speed data received: ${data.getData(DataType.SPEED)}")
                val speedData = data.getData(DataType.SPEED)
                trySendBlocking(MeasureMessage.MeasureData(speedData))
            }
        }

        Log.d("SensorRepository", "Registering speed callback")
        measureClient.registerMeasureCallback(DataType.SPEED, callback)

        awaitClose {
            Log.d("SensorRepository", "Unregistering speed callback")
            runBlocking {
                measureClient.unregisterMeasureCallbackAsync(DataType.SPEED, callback).await()
            }
        }
    }
}

sealed class MeasureMessage {
    class MeasureAvailability(val availability: DataTypeAvailability) : MeasureMessage()
    class MeasureData(val data: List<SampleDataPoint<Double>>) : MeasureMessage()
}