package com.shieldrone.station.service.camera

import android.graphics.Bitmap

interface ImageFrameProvider {
    fun startStream(callback: (Bitmap) -> Unit)
    fun stopStream()
}
