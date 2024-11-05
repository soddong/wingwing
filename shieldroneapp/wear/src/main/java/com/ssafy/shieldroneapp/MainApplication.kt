package com.ssafy.shieldroneapp

import android.app.Application
import android.content.Context
import com.ssafy.shieldroneapp.data.repository.DataRepository
import com.ssafy.shieldroneapp.data.repository.SensorRepository

class MainApplication : Application() {
    val sensorRepository by lazy { SensorRepository(this) }
    val dataRepository by lazy { DataRepository(this) }

    companion object {
        private lateinit var instance: MainApplication
        fun getContext(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}