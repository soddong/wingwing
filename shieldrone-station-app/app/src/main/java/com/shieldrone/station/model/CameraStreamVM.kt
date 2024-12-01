package com.shieldrone.station.model

import android.util.Log
import android.view.Surface
import androidx.lifecycle.ViewModel
import com.shieldrone.station.controller.CameraStreamController
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.CameraKey.KeyCameraVideoStreamSource
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.co_r.KeyExposureMode
import dji.sdk.keyvalue.key.co_r.KeyExposureModeRange
import dji.sdk.keyvalue.value.camera.CameraExposureMode
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.et.set
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.manager.interfaces.ICameraStreamManager.FrameFormat
import dji.v5.manager.interfaces.ICameraStreamManager.ScaleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CameraStreamVM : ViewModel() {

    private val TAG = "CameraStreamInfoVM"
    private val streamController = CameraStreamController()

    private val _cameraName = MutableStateFlow("Unknown")
    val cameraName: StateFlow<String> = _cameraName

    private val _frameInfo = MutableStateFlow("No frame data")
    val frameInfo: StateFlow<String> = _frameInfo

    private var cameraIndex = ComponentIndexType.LEFT_OR_MAIN
    val keyExposureModeRange = KeyTools.createKey(KeyExposureModeRange)
    val keyCameraVideoStreamSource = KeyTools.createKey(KeyCameraVideoStreamSource)
    val keyExposureMode = KeyTools.createKey(KeyExposureMode)

    private val frameListener = object : ICameraStreamManager.CameraFrameListener {
        override fun onFrame(
            frameData: ByteArray,
            offset: Int,
            length: Int,
            width: Int,
            height: Int,
            format: FrameFormat
        ) {
            _frameInfo.value = "Frame: ${width}x$height, Format: $format, ${String.format("%.1f", (if (length == 0) 1 else length)/1024.0)} bytes"
            streamController.sendFrameDataOverUDP(frameData, width, height)
        }
    }


    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared() called")
        removeFrameListener()
        streamController.closeSocket()
    }

    /**
     * 비동기적으로 카메라 모드를 PHOTO_NORMAL로 설정한 후 카메라 인덱스를 설정하는 함수
     */
    fun setCameraModeAndIndex(cameraIndex: ComponentIndexType) {
        setCameraModeToPhotoNormal {
            // 카메라 모드가 변경된 후, 인덱스를 설정합니다.
            setCameraIndex(cameraIndex)
        }
    }

    /**
     * 카메라 모드를 PHOTO_NORMAL로 설정하고 완료 시 콜백을 호출합니다.
     */
    private fun setCameraModeToPhotoNormal(onComplete: () -> Unit) {
        val keyCameraMode = CameraKey.KeyCameraMode.create()
        KeyManager.getInstance().setValue(
            keyCameraMode,
            CameraMode.PHOTO_NORMAL,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.d(TAG, "Camera mode set to PHOTO_NORMAL successfully.")
                    onComplete()
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "Failed to set camera mode: ${error.description()}")
                }
            }
        )
    }

    private fun setCameraIndex(cameraIndex: ComponentIndexType) {
        this.cameraIndex = cameraIndex
        KeyManager.getInstance().cancelListen(this)
        listenCameraType()
        addFrameListener()
    }

    private fun listenCameraType() {
        CameraKey.KeyCameraType.create(cameraIndex).listen(this) { result ->
            _cameraName.value = result?.toString() ?: "Not Supported"
        }
    }

    fun putCameraStreamSurface(surface: Surface, width: Int, height: Int, scaleType: ScaleType) {
        try {
            MediaDataCenter.getInstance().cameraStreamManager.putCameraStreamSurface(
                cameraIndex, surface, width, height, scaleType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to put camera stream surface: ${e.message}")
        }
    }

    fun removeCameraStreamSurface(surface: Surface) {
        try {
            MediaDataCenter.getInstance().cameraStreamManager.removeCameraStreamSurface(surface)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove camera stream surface: ${e.message}")
        }
    }

    fun addFrameListener() {
        MediaDataCenter.getInstance().cameraStreamManager.addFrameListener(
            cameraIndex, FrameFormat.NV21, frameListener
        )
    }

    fun removeFrameListener() {
        MediaDataCenter.getInstance().cameraStreamManager.removeFrameListener(frameListener)
    }

    fun getCameraVideoStreamSource() {
        keyCameraVideoStreamSource.listen(this, false, onChange = { newValue ->
            Log.d(TAG, "getCameraVideoStreamSource : ${newValue!!.name}")
        })
    }

    fun setCameraExposureMode() {
        keyExposureModeRange.listen(this, false, onChange = { newValue ->
            Log.d(TAG, "getCameraExposureModeRange : $newValue")

            // newValue가 null이 아니고 PROGRAM이 포함되어 있는지 확인
            if (newValue != null && newValue.stream().anyMatch { it == CameraExposureMode.PROGRAM }) {
                // 조건 충족 시 setCameraExposureModeRange 호출
                keyExposureMode.set(
                    CameraExposureMode.PROGRAM,
                    onSuccess = { ->

                        Log.d(TAG, "[View] 모드 설정 성공")
                    },
                    onFailure = {
                        Log.d(TAG, "[View] 모드 설정 실패")
                    }

                )
            }
        })

    }
}
