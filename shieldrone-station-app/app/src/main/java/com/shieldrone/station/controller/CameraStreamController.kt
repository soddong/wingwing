package com.shieldrone.station.controller

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class CameraStreamController {

    companion object {
        private const val HOST = "192.168.0.2"
        private const val PORT = 65432
        private const val MTU_SIZE = 1500 // MTU를 고려한 최대 패킷 크기 제한
        private const val JPEG_QUALITY = 70 // JPEG 압축 품질 (0 ~ 100)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var udpSocket: DatagramSocket? = null

    init {
        try {
            udpSocket = DatagramSocket()
            Log.i("StreamController", "UDP Socket created")
        } catch (e: Exception) {
            Log.e("StreamController", "Error creating UDP socket: ${e.message}")
        }
    }

    /**
     * NV21 형식의 프레임 데이터를 JPEG로 압축한 후 UDP로 전송합니다.
     */
    fun sendFrameDataOverUDP(frameData: ByteArray, originalWidth: Int, originalHeight: Int) {
        coroutineScope.launch {
            try {
                // NV21 데이터를 JPEG로 압축
                val jpegData = convertNV21ToJPEG(frameData, originalWidth, originalHeight)

                // 압축된 데이터가 MTU를 초과하면 전송하지 않음
                if (jpegData.size > MTU_SIZE) {
                    Log.w("StreamController", "JPEG data exceeds MTU limit, size: ${jpegData.size} bytes")
//                    return@launch
                }

                // UDP 패킷 생성 및 전송
                val packet = DatagramPacket(
                    jpegData,
                    jpegData.size,
                    InetAddress.getByName(HOST),
                    PORT
                )

                udpSocket?.send(packet)
                Log.i("StreamController", "Frame sent over UDP to $HOST:$PORT, size: ${jpegData.size} bytes")
            } catch (e: Exception) {
                Log.e("StreamController", "Error sending UDP packet: ${e.message}")
            }
        }
    }

    /**
     * NV21 형식의 데이터를 JPEG로 변환하는 함수
     */
    private fun convertNV21ToJPEG(data: ByteArray, width: Int, height: Int): ByteArray {
        return try {
            val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
            val outputStream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), JPEG_QUALITY, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e("StreamController", "Error converting NV21 to JPEG: ${e.message}")
            ByteArray(0)
        }
    }

    /**
     * UDP 소켓 닫기
     */
    fun closeSocket() {
        try {
            udpSocket?.close()
            Log.i("StreamController", "UDP Socket closed")
        } catch (e: Exception) {
            Log.e("StreamController", "Error closing UDP socket: ${e.message}")
        } finally {
            udpSocket = null
        }
    }
}
