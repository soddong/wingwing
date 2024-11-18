package com.ssafy.shieldroneapp

import android.app.Application
import android.content.Context
import com.ssafy.shieldroneapp.domain.repository.DataRepository
import com.ssafy.shieldroneapp.domain.repository.SensorRepository
import com.ssafy.shieldroneapp.data.remote.WearConnectionManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {
    @Inject
    lateinit var wearConnectionManager: WearConnectionManager

    @Inject 
    lateinit var dataRepository: DataRepository

    @Inject 
    lateinit var sensorRepository: SensorRepository

    companion object {
        private lateinit var instance: MainApplication
        fun getContext(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        GlobalScope.launch {
            wearConnectionManager.initialize()
        }
    }
}