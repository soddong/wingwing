package com.shieldrone.station.service.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dji.sdk.keyvalue.value.common.ComponentIndexType
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
        private const val FRAME_WIDTH = 640
        private const val FRAME_HEIGHT = 640
    }

    override fun startStream(callback: (Bitmap) -> Unit) {
        Log.i("Drone", "startStream")

        val cameraStreamManager: ICameraStreamManager =
            MediaDataCenter.getInstance().cameraStreamManager

        // CameraFrameListener를 추가하여 프레임 데이터 수신
        cameraStreamManager.addFrameListener(
            ComponentIndexType.LEFT_OR_MAIN,
            ICameraStreamManager.FrameFormat.NV21
        ) { frameData, offset, length, width, height, format ->
            // 수신한 프레임 데이터를 처리하고 화면에 출력
            processFrameData(frameData, offset, length, width, height, format, callback)
        }
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

    fun stopReceivingFrames() {
        val cameraStreamManager = MediaDataCenter.getInstance().cameraStreamManager

//        cameraStreamManager.removeFrameListener()
    }
}
