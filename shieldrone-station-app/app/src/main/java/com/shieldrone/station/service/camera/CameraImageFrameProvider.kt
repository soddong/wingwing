package com.shieldrone.station.service.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer

class CameraImageFrameProvider(private val context: Context) : ImageFrameProvider {
    private var cameraProvider: ProcessCameraProvider? = null

    override fun startStream(callback: (Bitmap) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                val bitmap = imageProxy.toBitmap()
                bitmap?.let { callback(it) }
                imageProxy.close()
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                context as androidx.lifecycle.LifecycleOwner,
                cameraSelector,
                imageAnalysis
            )
            Log.i("CameraImageFrameProvider", "Camera bound to lifecycle")
        }, ContextCompat.getMainExecutor(context))
    }

    override fun stopStream() {
        cameraProvider?.unbindAll()
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        val buffer: ByteBuffer = planes[0].buffer
        val byteArray = ByteArray(buffer.remaining())
        buffer.get(byteArray)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}
