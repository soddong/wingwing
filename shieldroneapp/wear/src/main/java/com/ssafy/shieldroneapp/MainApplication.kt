package com.ssafy.shieldroneapp

import android.app.Application
import com.ssafy.shieldroneapp.data.repository.SensorRepository

const val TAG = "shieldrone WearOS Application"

class MainApplication : Application() {
    val sensorRepository by lazy { SensorRepository(this) }
}