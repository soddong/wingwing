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
        private const val TAG = "모바일: 베이스 서비스"
        const val ACTION_ENABLE_DATA_LISTENING = "com.ssafy.shieldroneapp.ACTION_ENABLE_DATA_LISTENING"
        const val ACTION_DISABLE_DATA_LISTENING = "com.ssafy.shieldroneapp.ACTION_DISABLE_DATA_LISTENING"
    }

    protected val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    protected var isDataListeningEnabled = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "서비스 생성: ${this.javaClass.simpleName}")
        initializeService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand: ${this.javaClass.simpleName}")

        // 워치에서 모바일로 데이터 수신 제어 액션 처리
        when (intent?.action) {
            ACTION_ENABLE_DATA_LISTENING -> {
                isDataListeningEnabled = true
                Log.d(TAG, "데이터 수신 활성화")
            }
            ACTION_DISABLE_DATA_LISTENING -> {
                isDataListeningEnabled = false
                Log.d(TAG, "데이터 수신 비활성화")
            }
        }

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