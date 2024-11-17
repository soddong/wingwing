package com.ssafy.shieldroneapp.permissions

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import javax.inject.Inject

class PermissionManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "모바일: 권한 매니저"
    }
    
    //  녹음 권한
    fun hasAudioPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "오디오 권한 체크: $hasPermission")
        return hasPermission
    }
}