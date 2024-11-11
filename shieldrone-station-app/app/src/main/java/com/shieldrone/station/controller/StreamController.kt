package com.shieldrone.station.controller

import android.graphics.Bitmap
import android.util.Log
import com.shieldrone.station.service.camera.ImageFrameProvider
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class StreamController(private val imageFrameProvider: ImageFrameProvider) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var udpSocket: DatagramSocket? = null
    private var onFrameAvailable: ((Bitmap) -> Unit)? = null

    companion object {
        private const val HOST = "192.168.253.189"
        private const val PORT = 65432
        private const val FRAME_WIDTH = 640
        private const val FRAME_HEIGHT = 640
    }

    init {
        try {
            udpSocket = DatagramSocket()
            Log.i("StreamController", "UDP Socket created")
        } catch (e: Exception) {
            Log.e("StreamController", "Error creating UDP socket: ${e.message}")
        }
    }

    fun startLive() {
        imageFrameProvider.startStream { bitmap ->
            processFrame(bitmap)
        }
    }

    fun stopLive() {
        imageFrameProvider.stopStream()
        closeSocket()
    }

    fun setOnFrameAvailableListener(callback: (Bitmap) -> Unit) {
        onFrameAvailable = callback
    }

    private fun processFrame(bitmap: Bitmap) {
        Log.d("StreamController", "Processing frame")
        sendBitmapOverUDP(bitmap)
    }

    private fun sendBitmapOverUDP(bitmap: Bitmap) {
        coroutineScope.launch {
            try {
                // 이미지 크기 조정 (320x240 픽셀로 축소)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, FRAME_WIDTH, FRAME_HEIGHT, true)

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
                Log.i("StreamController", "Frame sent over UDP to $HOST:$PORT, size: ${byteArray.size} bytes")

            } catch (e: Exception) {
                Log.e("StreamController", "Error sending UDP packet: ${e.message}")
            }
        }
    }

    private fun closeSocket() {
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
