package com.sheildron.station.src

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class VideoController(private val context: Context) {

    private var udpSocket: DatagramSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val HOST = "192.168.240.189"
        private const val PORT = 65432
    }

    init {
        try {
            udpSocket = DatagramSocket()
            Log.i("VideoController", "UDP Socket created")
        } catch (e: Exception) {
            Log.e("VideoController", "Error creating UDP socket: ${e.message}")
        }
    }

    fun startCameraPreview() {
        Log.i("VideoController", "startCameraPreview called")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(context as androidx.lifecycle.LifecycleOwner, cameraSelector, imageAnalysis)
            Log.i("VideoController", "Camera bound to lifecycle")
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toBitmap()
            bitmap?.let {
                sendBitmapOverUDP(it)
            }
        } catch (e: Exception) {
            Log.e("VideoController", "Error processing image: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        val buffer: ByteBuffer = planes[0].buffer
        val byteArray = ByteArray(buffer.remaining())
        buffer.get(byteArray)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    private fun sendBitmapOverUDP(bitmap: Bitmap) {
        coroutineScope.launch {
            try {
                // 이미지 크기 조정 (320x240 픽셀로 축소)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 320, 240, true)

                // 압축 및 바이트 배열로 변환 (품질을 50으로 설정하여 크기 줄이기)
                val byteArrayOutputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                // 패킷이 MTU를 초과하지 않도록 크기 확인
                if (byteArray.size > 1500) {
                    Log.w("VideoController", "Warning: Packet size may exceed MTU limit")
                }

                val packet = DatagramPacket(
                    byteArray,
                    byteArray.size,
                    InetAddress.getByName(HOST),
                    PORT
                )

                udpSocket?.send(packet)
                Log.i("VideoController", "Frame sent over UDP to $HOST:$PORT, size: ${byteArray.size} bytes")

            } catch (e: Exception) {
                Log.e("VideoController", "Error sending UDP packet: ${e.message}")
            }
        }
    }


    fun closeSocket() {
        try {
            udpSocket?.close()
            Log.i("VideoController", "UDP Socket closed")
        } catch (e: Exception) {
            Log.e("VideoController", "Error closing UDP socket: ${e.message}")
        } finally {
            udpSocket = null
        }
    }
}
