package com.shieldrone.station.controller

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import com.shieldrone.station.constant.CameraConstant
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class CameraStreamController {

    private var udpSocket: DatagramSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // 마지막으로 전송된 프레임의 타임스탬프
    private var lastSentTime = System.currentTimeMillis()

    // 전송 간격 (30fps의 모든 프레임을 보내지 않기 위해 샘플링)
    private val frameIntervalMs = 50 // 100ms 간격으로 전송 (약 10fps)

    init {
        initializeSocket()
    }

    private fun initializeSocket() {
        try {
            if (udpSocket == null || udpSocket?.isClosed == true) {
                udpSocket = DatagramSocket()
                Log.i("StreamController", "UDP Socket created")
            }
        } catch (e: Exception) {
            Log.e("StreamController", "Error creating UDP socket: ${e.message}")
        }
    }

    /**
     * NV21 형식의 프레임 데이터를 JPEG로 압축한 후 일정 주기로 UDP로 전송합니다.
     */
    fun sendFrameDataOverUDP(frameData: ByteArray, originalWidth: Int, originalHeight: Int) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSentTime < frameIntervalMs) {
            return // 전송 주기 내에서는 전송하지 않음
        }
        lastSentTime = currentTime

        coroutineScope.launch {
            val jpegData = convertNV21ToJPEG(frameData, originalWidth, originalHeight)
            if (jpegData.isEmpty()) return@launch

            try {
                val packet = DatagramPacket(
                    jpegData,
                    jpegData.size,
                    InetAddress.getByName(CameraConstant.HOST),
                    CameraConstant.PORT
                )
                udpSocket?.send(packet)
                Log.d("StreamController", "Frame sent to ${CameraConstant.HOST}:${CameraConstant.PORT}, size: ${jpegData.size} bytes")
            } catch (e: Exception) {
                Log.e("StreamController", "Error sending UDP packet: ${e.message}")
            }
        }
    }

    /**
     * NV21 형식의 데이터를 JPEG로 변환하고 압축합니다.
     */
    private fun convertNV21ToJPEG(data: ByteArray, width: Int, height: Int): ByteArray {
        return ByteArrayOutputStream().use { outputStream ->
            try {
                val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
                yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, outputStream)

                val jpegData = outputStream.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
                val resizedBitmap = Bitmap.createScaledBitmap(
                    bitmap, CameraConstant.RESIZED_WIDTH, CameraConstant.RESIZED_HEIGHT, true
                )

                ByteArrayOutputStream().use { resizedOutputStream ->
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, CameraConstant.JPEG_QUALITY, resizedOutputStream)
                    resizedBitmap.recycle()
                    return resizedOutputStream.toByteArray()
                }
            } catch (e: Exception) {
                Log.e("StreamController", "Error converting NV21 to JPEG: ${e.message}")
                ByteArray(0)
            }
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
