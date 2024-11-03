package com.ssafy.shieldroneapp

import android.app.Application
import com.ssafy.shieldroneapp.data.repository.DataRepository
import com.ssafy.shieldroneapp.data.repository.SensorRepository

const val TAG = "shieldrone WearOS Application"

class MainApplication : Application() {
    val sensorRepository by lazy { SensorRepository(this) }
    val dataRepository by lazy { DataRepository(this) }
}