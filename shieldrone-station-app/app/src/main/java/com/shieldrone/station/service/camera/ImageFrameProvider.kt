package com.shieldrone.station.src.service.camera

import android.graphics.Bitmap

interface ImageFrameProvider {
    fun startStream(callback: (Bitmap) -> Unit)
    fun stopStream()
}