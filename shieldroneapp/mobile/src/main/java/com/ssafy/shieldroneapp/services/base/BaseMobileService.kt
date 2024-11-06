package com.ssafy.shieldroneapp.services.base

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * 모든 모바일 서비스를 위한 기본 서비스 클래스.
 *
 * 공통 로직이나 상속을 위한 기본 클래스로, 다른 서비스가 이 클래스를 상속받아 공통 기능을 재사용할 수 있다.
 * 주로 서비스의 시작, 중지 등의 공통적인 동작과 서비스 생명주기 관리 로직을 포함한다.
 */
abstract class BaseMobileService : WearableListenerService() {

    companion object {
        private const val TAG = "BaseMobileService"
    }

    protected val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate: ${this.javaClass.simpleName}")
        initializeService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand: ${this.javaClass.simpleName}")
        handleStart(intent)
        // 서비스가 강제 종료되면 재시작하도록
        return Service.START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy: ${this.javaClass.simpleName}")
        serviceScope.cancel()
        cleanup()
        super.onDestroy()
    }

    protected open fun initializeService() {
        //
    }

    protected open fun handleStart(intent: Intent?) {
        //
    }

    protected open fun cleanup() {
        //
    }

    protected fun handleError(error: Throwable, message: String) {
        Log.e(TAG, "$message: ${error.message}", error)
        //
    }

    protected fun isServiceRunning(): Boolean {
        //
        return true
    }
}