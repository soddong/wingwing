package com.ssafy.shieldroneapp.services

import android.content.Context
import android.os.PowerManager
import android.util.Log

class WakeLockManager private constructor(context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "WakeLockManager"
        @Volatile private var instance: WakeLockManager? = null

        fun getInstance(context: Context): WakeLockManager {
            return instance ?: synchronized(this) {
                instance ?: WakeLockManager(context).also { instance = it }
            }
        }
    }

    fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) {
            return
        }

        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ShieldDrone:DimScreen"
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
        Log.d(TAG, "WakeLock acquired")
    }

    fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }
}
