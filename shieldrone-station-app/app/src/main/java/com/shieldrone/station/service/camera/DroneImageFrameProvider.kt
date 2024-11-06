package com.shieldrone.station.service.camera

import android.content.Context
import android.graphics.Bitmap

// TODO: DJI SDK 연결 후, 작업 필요
class DroneImageFrameProvider(private val context: Context) : ImageFrameProvider {
    override fun startStream(callback: (Bitmap) -> Unit) {
    }

    override fun stopStream() {
    }
}
