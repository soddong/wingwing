package com.shieldrone.station.service.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.camera.VideoFrameRate
import dji.sdk.keyvalue.value.camera.VideoResolution
import dji.sdk.keyvalue.value.camera.VideoResolutionFrameRate
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.manager.interfaces.ICameraStreamManager.FrameFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

// TODO: DJI SDK 연결 후, 작업 필요
class DroneImageFrameProvider(private val context: Context) : ImageFrameProvider {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var udpSocket: DatagramSocket? = null

    companion object {
        private const val HOST = "192.168.0.2"
        private const val PORT = 65432
        private const val FRAME_WIDTH = 1920
        private const val FRAME_HEIGHT = 1080
    }

    override fun startStream(callback: (Bitmap) -> Unit) {
        Log.i("Drone", "startStream")

        val cameraStreamManager: ICameraStreamManager =
            MediaDataCenter.getInstance().cameraStreamManager
        // 1. 카메라 모드를 VIDEO_NORMAL로 설정
        setCameraModeToVideoNormal {
            // 2. 해상도와 프레임 레이트를 설정
            setVideoResolutionFrameRate {
                // 3. 모든 설정이 완료된 후 프레임 리스너 추가
                cameraStreamManager.addFrameListener(
                    ComponentIndexType.LEFT_OR_MAIN,
                    FrameFormat.NV21
                ) { frameData, offset, length, width, height, format ->
                    processFrameData(frameData, offset, length, width, height, format, callback)
                }
                Log.i("Drone", "Camera mode and resolution set, stream listener added.")
            }
        }
    }

    // 비동기 카메라 모드 설정 메서드
    private fun setCameraModeToVideoNormal(onComplete: () -> Unit) {
        val keyCameraMode = CameraKey.KeyCameraMode.create()
        KeyManager.getInstance().setValue(
            keyCameraMode,
            CameraMode.VIDEO_NORMAL,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.d("CameraMode", "Camera mode set to VIDEO_NORMAL successfully.")
                    onComplete() // 성공 시 다음 단계 호출
                }

                override fun onFailure(error: IDJIError) {
                    Log.e("CameraMode", "Failed to set camera mode: ${error.description()}")
                }
            })
    }

    // 비동기 해상도 및 프레임 레이트 설정 메서드
    private fun setVideoResolutionFrameRate(onComplete: () -> Unit) {
        val videoResolutionFrameRate =
            VideoResolutionFrameRate(
                VideoResolution.RESOLUTION_1920x1080,
                VideoFrameRate.RATE_24FPS
            )
        val keyVideoResolutionFrameRate = CameraKey.KeyVideoResolutionFrameRate.create()

        KeyManager.getInstance().setValue(
            keyVideoResolutionFrameRate,
            videoResolutionFrameRate,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.d(
                        "CameraSettings",
                        "Video resolution and frame rate set successfully to 640x512 at 30FPS."
                    )
                    onComplete() // 성공 시 다음 단계 호출
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(
                        "CameraSettings",
                        "Failed to set video resolution and frame rate: ${error.description()}"
                    )
                }
            })
    }


    override fun stopStream() {
    }

    /**
     * YUV (NV21) 데이터를 RGB로 변환하는 함수
     *
     * @param yuvData YUV 형식의 데이터 (NV21)
     * @param width 이미지의 가로 길이
     * @param height 이미지의 세로 길이
     * @return 변환된 RGB 형식의 Bitmap
     */
    private fun convertYUVToRGB(yuvData: ByteArray, width: Int, height: Int): Bitmap {
        val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
        yuvMat.put(0, 0, yuvData)

        val rgbMat = Mat()
        Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgbMat, bitmap)

        yuvMat.release()
        rgbMat.release()

        return bitmap
    }

    /**
     * 프레임 데이터를 처리하고, 변환된 이미지를 callback으로 전달하여 화면에 출력하는 함수
     *
     * @param frameData 수신된 YUV 형식의 프레임 데이터
     * @param width 이미지의 가로 길이
     * @param height 이미지의 세로 길이
     * @param callback 변환된 이미지를 전달할 callback 함수
     */
    private fun processFrameData(
        frameData: ByteArray,
        offset: Int,
        length: Int,
        width: Int,
        height: Int,
        format: FrameFormat,
        callback: (Bitmap) -> Unit
    ) {
        Log.i("DroneImageFrameProvider", "Received frame data size: ${frameData.size}")
        // YUV 데이터를 RGB로 변환
        val bitmap = convertYUVToRGB(frameData, width, height)
        // 변환된 Bitmap을 callback을 통해 전달하여 화면에 표시
        callback(bitmap)
        sendBitmapOverUDP(bitmap)
    }

    private fun sendBitmapOverUDP(bitmap: Bitmap) {
        coroutineScope.launch {
            try {
                // 이미지 크기 조정 (320x240 픽셀로 축소)
                val resizedBitmap =
                    Bitmap.createScaledBitmap(bitmap, FRAME_WIDTH, FRAME_HEIGHT, true)

                // 압축 및 바이트 배열로 변환 (품질을 50으로 설정하여 크기 줄이기)
                val byteArrayOutputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                // 패킷이 MTU를 초과하지 않도록 크기 확인
                if (byteArray.size > 1500) {
                    Log.w("StreamController", "Warning: Packet size may exceed MTU limit")
                }

                val packet = DatagramPacket(
                    byteArray,
                    byteArray.size,
                    InetAddress.getByName(HOST),
                    PORT
                )

                udpSocket?.send(packet)
                Log.i(
                    "StreamController",
                    "Frame sent over UDP to $HOST:$PORT, size: ${byteArray.size} bytes"
                )

            } catch (e: Exception) {
                Log.e("StreamController", "Error sending UDP packet: ${e.message}")
            }
        }
    }
}
