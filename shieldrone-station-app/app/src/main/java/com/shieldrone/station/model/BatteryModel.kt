package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.et.get
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class BatteryModel {
    // 1. field, companion object

    companion object {
        val batteryPercent by lazy {
            KeyTools.createKey(BatteryKey.KeyLifetimeRemainingInPercent).get()
        }
    }

}