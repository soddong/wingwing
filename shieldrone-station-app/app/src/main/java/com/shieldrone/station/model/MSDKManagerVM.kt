package com.shieldrone.station.model

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback


class MSDKManagerVM : ViewModel() {
    // The data is held in livedata mode, but you can also save the results of the sdk callbacks any way you like.

    fun initMobileSDK(appContext: Context) {
        Log.i("MSDKManagerVM", "MSDK 실행")
        // Initialize and set the sdk callback, which is held internally by the sdk until destroy() is called
        SDKManager.getInstance().init(appContext, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                Log.i("MSDKManagerVM", "등록 성공")
            }

            override fun onRegisterFailure(error: IDJIError) {
                Log.i("MSDKManagerVM", "등록 실패")

            }

            override fun onProductDisconnect(productId: Int) {
                Log.i("MSDKManagerVM", "연결 끊김")

            }

            override fun onProductConnect(productId: Int) {
                Log.i("MSDKManagerVM", "연결됨 + ${productId}")

            }

            override fun onProductChanged(productId: Int) {
            }

            override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                Log.i("MSDKManagerVM", "Init 실행")

                // Don't forget to call the registerApp()
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    Log.i("MSDKManagerVM", "registerApp")

                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
            }
        })
    }

    fun destroyMobileSDK() {
        SDKManager.getInstance().destroy()
    }

}