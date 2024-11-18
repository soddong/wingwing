package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.shieldrone.station.data.Position
import com.shieldrone.station.data.State
import com.shieldrone.station.data.TrackingDataDiff
import dji.sdk.keyvalue.key.co_v.KeyAircraftLocation3D
import dji.sdk.keyvalue.key.co_v.KeyUltrasonicHeight
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.et.listen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

class FlightAutoControlVM : ViewModel() {
    private val TAG = "FlightAutoControlVM"
    // 1. 라이브데이터 및 필요한 필드
    private val flightControlModel = FlightAutoControlModel()
    private val handler = Handler(Looper.getMainLooper())

    // StateFlow 변수 선언
    private val _droneState = MutableStateFlow<State?>(null)
    val droneState: StateFlow<State?> get() = _droneState.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> get() = _status.asStateFlow()

    private val _targetPosition = MutableStateFlow<Position?>(null)
    val targetPosition: StateFlow<Position?> get() = _targetPosition.asStateFlow()

    private val _virtualStickState = MutableStateFlow<String?>(null)
    val virtualStickState: StateFlow<String?> get() = _virtualStickState.asStateFlow()

    private val _sonicHeight = MutableStateFlow<Int?>(null)
    val sonicHeight: StateFlow<Int?> get() = _sonicHeight.asStateFlow()

    lateinit var trackingData: StateFlow<TrackingDataDiff?>

    private var autoControlCoroutine: Job? = null

    var pitch: Int by mutableStateOf(0)
        private set


    // 2. 필요한 초기화
    init {
        flightControlModel.subscribeDroneState { state ->
            _droneState.value = state
        }
        flightControlModel.subscribeVirtualStickState { stickState ->
            _virtualStickState.value = stickState
        }
        KeyUltrasonicHeight.create().listen(this,false, { newValue ->
            _sonicHeight.value = newValue
        })
    }

    fun setTrackingInfo(trackingDataDiffFlow: StateFlow<TrackingDataDiff?>){
        trackingData = trackingDataDiffFlow
    }

    // 3. 생명주기 관리

    // 생명주기 관리
    override fun onCleared() {
        Log.d(TAG, "[ViewModel] onCleared 시작")
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "[ViewModel] onCleared 성공")
    }

    // 4. 드론 이륙 및 착륙

    fun startTakeOff() {
        Log.d(TAG, "[ViewModel] startTakeOff 시작")
        flightControlModel.startTakeOff(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _status.value = "이륙이 시작되었습니다."
                Log.d(TAG, "[ViewModel] startTakeOff 성공")
                flightControlModel.monitorTakeoffStatus()
            }

            override fun onFailure(error: IDJIError) {
                _status.value = "이륙 실패: ${error.description()}"
                Log.d(TAG, "[ViewModel] startTakeOff 실패: ${error.description()}")
            }
        })
    }

    // 순항 고도로 상승
    fun ascendToCruiseAltitude(altitudeSpeed: Int, targetAltitude: Float = 3.2f) {
        Log.d(TAG, "[ViewModel] ascendToCruiseAltitude 시작")
        CoroutineScope(Dispatchers.Default).launch {
            try {
                _status.value = "순항 고도 상승시작"
                adjustAltitudeCoroutine(altitudeSpeed, targetAltitude)
                Log.d(TAG, "[ViewModel] ascendToCruiseAltitude 성공")
            } catch (e: Exception) {
                Log.d(TAG, "[ViewModel] ascendToCruiseAltitude 실패: ${e.message}")
            }
        }
    }

    suspend fun adjustAltitudeCoroutine(altitudeSpeed: Int, targetAltitude: Float) {
        Log.d(TAG, "[ViewModel] adjustAltitudeCoroutine 시작")
        try {
            _status.value = "순항 고도 상승중"
            while (true) {
                val currentAltitude = KeyAircraftLocation3D.create().get()
                if (
//                    currentAltitude == null
                    sonicHeight.value == null
                    ){
                    Log.d(TAG, "[ViewModel] adjustAltitudeCoroutine 실패 : 고도 정보 없음")
                    _status.value = "고도 정보 없음"
                    flightControlModel.adjustAltitude(0)
                    break
                }
//                val curAltitude = currentAltitude!!.altitude
                val curAltitude:Float = sonicHeight.value!!/10f
                if (curAltitude>= targetAltitude) {
                    Log.d(TAG, "[ViewModel] adjustAltitudeCoroutine 성공: 목표 고도 도달")
                    flightControlModel.adjustAltitude(0)
                    _status.value = "순항 고도 도달"
                    break
                }

                val altitudeRatio = (curAltitude- 1.2f) / (targetAltitude -1.2f)
                val adjustmentSpeed = when {
                    altitudeRatio < 0.2 -> (altitudeSpeed * 0.3).toLong()
                    altitudeRatio < 0.8 -> altitudeSpeed
                    else -> (altitudeSpeed * 0.2).toLong()
                }

                flightControlModel.adjustAltitude(adjustmentSpeed.toInt())
                delay(200L)
            }
        } catch (e: Exception) {
            Log.d(TAG, "[ViewModel] adjustAltitudeCoroutine 실패: ${e.message}")
            flightControlModel.adjustAltitude(0)
        }
    }

    fun startLanding() {
        Log.d(TAG, "[ViewModel] startLanding 시작")
        flightControlModel.startLanding(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _status.value = "착륙이 시작되었습니다."
                Log.d(TAG, "[ViewModel] startLanding 성공")
            }

            override fun onFailure(error: IDJIError) {
                _status.value = "착륙 실패: ${error.description()}"
                Log.d(TAG, "[ViewModel] startLanding 실패: ${error.description()}")
            }
        })
    }



    // 7. 가상 스틱 활성화 및 비 활성화 메서드
    /**
     * Virtual Stick 모드 활성화
     */
    fun enableVirtualStickMode() {
        Log.d(TAG, "[ViewModel] enableVirtualStickMode 시작")
        flightControlModel.enableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.d(TAG, "[ViewModel] enableVirtualStickMode 성공")
            }

            override fun onFailure(error: IDJIError) {
                Log.d(TAG, "[ViewModel] enableVirtualStickMode 실패: ${error.description()}")
            }
        })
    }

    fun disableVirtualStickMode() {
        Log.d(TAG, "[ViewModel] disableVirtualStickMode 시작")
        flightControlModel.disableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.d(TAG, "[ViewModel] disableVirtualStickMode 성공")
            }

            override fun onFailure(error: IDJIError) {
                Log.d(TAG, "[ViewModel] disableVirtualStickMode 실패: ${error.description()}")
            }
        })
    }

    fun adjustYaw(yawDifference: Double) {
        Log.d(TAG, "[ViewModel] adjustYaw 시작")
        try {
            flightControlModel.adjustYaw(yawDifference)
//            Log.d(TAG, "[ViewModel] adjustYaw 성공")
        } catch (e: Exception) {
            Log.d(TAG, "[ViewModel] adjustYaw 실패: ${e.message}")
        }
    }

    fun setTargetPosition(position: Position) {
        Log.d(TAG, "[ViewModel] setTargetPosition 시작")
        _targetPosition.value = position
        Log.d(TAG, "[ViewModel] setTargetPosition 성공: position=$position")
    }

    fun updatePitch(value: Int) {
        Log.d(TAG, "[ViewModel] updatePitch 시작")
        pitch = value
        Log.d(TAG, "[ViewModel] updatePitch 성공: pitch=$value")
    }

    fun adjustPitch() {
        Log.d(TAG, "[ViewModel] adjustPitch 시작")
        try {
            flightControlModel.adjustPitch(pitch)
//            Log.d(TAG, "[ViewModel] adjustPitch 성공")
        } catch (e: Exception) {
            Log.d(TAG, "[ViewModel] adjustPitch 실패: ${e.message}")
        }
    }

    fun startAutoControl(targetAltitude:Float, altitudeThreshold:Float=0.1f, yawThreshold: Float =0.2f, controlDelay:Long = 100L ){
        stopAutoControl()
        autoControlCoroutine = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                adjustAutoControl(targetAltitude,altitudeThreshold, yawThreshold)
                delay(controlDelay)
            }
        }
    }

    fun stopAutoControl(){
        if(autoControlCoroutine != null && autoControlCoroutine!!.isActive){
            autoControlCoroutine!!.cancel()
        }
    }

    fun adjustAutoControl(targetAltitude:Float, altitudeThreshold:Float=0.1f, yawThreshold: Float =0.2f,
                          maxYawPower: Double = 220.0,minYawPower:Double = 10.0, kpValue: Double = 1.5,
                          maxPitchPower: Double = 30.0) {
        var altitudePower = 0.0
        var yawPower = 0.0
        var pitchPower = 0.0


        //쓰로틀 계산, 순항 고도 유지
//        val currentAltitude = KeyAircraftLocation3D.create().get()?.altitude
        val currentAltitude = sonicHeight.value
        if(currentAltitude != null){
//            val altitudeDifference = targetAltitude - currentAltitude
            val altitudeDifference = targetAltitude - currentAltitude.toDouble()/10f

            if (abs(altitudeDifference) > altitudeThreshold) {
                altitudePower = altitudeDifference * 10
            }
        }


        //yaw 계산
        val offsetX = trackingData.value?.newData?.normalizedOffsetX
        if(offsetX != null){
            val absOffsetX = abs(offsetX)

            Log.d("TrackingTargetActivity", "OffsetX: $offsetX, AbsOffsetX: $absOffsetX")

            if (absOffsetX > yawThreshold) {
                // 임계값을 초과한 부분을 0부터 1 사이로 정규화
                val scaledOffset = (absOffsetX - yawThreshold) / (1.0 - yawThreshold)
                // 최소 및 최대 회전 속도 사이에서 보간
                val adjustmentValue = scaledOffset * (maxYawPower - minYawPower) + minYawPower
                Log.d("TrackingTargetActivity", "adjustmentValue: $adjustmentValue")
                // 방향에 따라 부호 적용
                yawPower = adjustmentValue * sign(offsetX)
            }
        }


        //pitch 계산
        val eYFuture = trackingData.value?.futureErrorY
        if(eYFuture != null){
            val vY = -kpValue * eYFuture
            pitchPower = vY.coerceIn(-1.0, 1.0) * maxPitchPower
        }
        _status.value = "자동 제어 진행중 alititude : ${String.format("%.1f", altitudePower)} yaw : ${String.format("%.1f", yawPower)}, pitch : ${String.format("%.1f", pitchPower)}"

        flightControlModel.adjustAutoControl(altitudePower, yawPower, pitchPower)
    }
}
