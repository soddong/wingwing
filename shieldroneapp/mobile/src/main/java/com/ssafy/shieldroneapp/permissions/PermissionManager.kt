package com.ssafy.shieldroneapp.permissions

import android.content.Context
import androidx.core.content.ContextCompat
import javax.inject.Inject

class PermissionManager @Inject constructor(
    private val context: Context
) {
    //  음성 권한
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}