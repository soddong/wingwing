package com.shieldrone.station

import android.app.Application
import android.util.Log
import com.shieldrone.station.model.MSDKManagerVM
import com.shieldrone.station.model.globalViewModels

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/3/1
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
open class DJIApplication : Application() {

    private val msdkManagerVM: MSDKManagerVM by globalViewModels()

    override fun onCreate() {
        super.onCreate()
        Log.i("DJIApplication", "앱 초기화")

        // Ensure initialization is called first
        msdkManagerVM.initMobileSDK(this)

    }

}

