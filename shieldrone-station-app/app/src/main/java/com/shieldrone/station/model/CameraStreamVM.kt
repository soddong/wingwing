package com.shieldrone.station.model

import android.util.Log
import android.view.Surface
import androidx.lifecycle.ViewModel
import com.shieldrone.station.controller.CameraStreamController
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.manager.interfaces.ICameraStreamManager.ScaleType
import dji.v5.manager.interfaces.ICameraStreamManager.FrameFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CameraStreamVM : ViewModel() {

    private val TAG = "CameraStreamInfoVM"
    private val streamController = CameraStreamController()

    private val _cameraName = MutableStateFlow("Unknown")
    val cameraName: StateFlow<String> = _cameraName

    private val _streamInfo = MutableStateFlow("")
    val streamInfo: StateFlow<String> = _streamInfo

    private val _frameInfo = MutableStateFlow("No frame data")
    val frameInfo: StateFlow<String> = _frameInfo

    private var cameraIndex = ComponentIndexType.LEFT_OR_MAIN

    private val frameListener = object : ICameraStreamManager.CameraFrameListener {
        override fun onFrame(
            frameData: ByteArray,
            offset: Int,
            length: Int,
            width: Int,
            height: Int,
            format: FrameFormat
        ) {

            // 프레임 데이터를 UDP로 전송
            streamController.sendFrameDataOverUDP(frameData, width, height)

            Log.d(TAG, "Frame received: $length bytes, Resolution: ${width}x$height")
            CoroutineScope(Dispatchers.Main).launch {
                _frameInfo.value = "Frame: ${width}x$height, Format: $format"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared() called")
        removeFrameListener()
        streamController.closeSocket()
    }

    fun setCameraIndex(cameraIndex: ComponentIndexType) {
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
}
