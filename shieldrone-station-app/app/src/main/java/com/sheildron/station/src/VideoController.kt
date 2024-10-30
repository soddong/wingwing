package com.sheildron.station.src

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import java.nio.ByteBuffer
import kotlin.concurrent.thread


class VideoController(private val context: Context) {

    private var socket: Socket? = null
    private var dataOutputStream: DataOutputStream? = null

    companion object {
        private const val HOST = "192.168.0.12" // Replace with actual IP
        private const val PORT = 65432
    }


    fun pingServer(ipAddress: String, callback: (Boolean) -> Unit) {
        thread {
            try {
                val address = InetAddress.getByName(ipAddress)
                val reachable = address.isReachable(2000)  // 타임아웃 2초 설정
                callback(reachable)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }


    // Initialize camera preview and network transmission
    fun startCameraPreview() {
        Log.i("VideoController", "startCameraPreview called")

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e("VideoController", "Camera permission not granted")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1280, 720))
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

    // Process image frame and send over network
    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toBitmap()
            bitmap?.let {
                sendBitmapOverNetwork(it)
            }
        } catch (e: Exception) {
            Log.e("VideoController", "Error processing image: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    // Convert ImageProxy to Bitmap
    private fun ImageProxy.toBitmap(): Bitmap? {
        val buffer: ByteBuffer = planes[0].buffer
        val byteArray = ByteArray(buffer.remaining())
        buffer.get(byteArray)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    // Compress and send bitmap over network
    private fun sendBitmapOverNetwork(bitmap: Bitmap) {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            if (socket == null || socket?.isClosed == true) {
                socket = Socket(InetAddress.getByName(HOST), PORT)
                dataOutputStream = DataOutputStream(socket?.getOutputStream())
                Log.i("VideoController", "Connected to socket at $HOST:$PORT")
            }

            dataOutputStream?.writeInt(byteArray.size)
            dataOutputStream?.write(byteArray)
            dataOutputStream?.flush()

        } catch (e: Exception) {
            Log.e("VideoController", "Error sending data: ${e.message}")
            closeSocket()
        }
    }

    private fun closeSocket() {
        try {
            dataOutputStream?.close()
            socket?.close()
            Log.i("VideoController", "Socket closed")
        } catch (e: Exception) {
            Log.e("VideoController", "Error closing socket: ${e.message}")
        } finally {
            socket = null
            dataOutputStream = null
        }
    }
}
