package com.ssafy.shieldroneapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtils {

    // EncryptedSharedPreferences 생성
    fun getEncryptedSharedPreferences(context: Context, fileName: String): SharedPreferences {
        return EncryptedSharedPreferences.create(
            fileName,
            MasterKey.DEFAULT_MASTER_KEY_ALIAS, // 기본 alias 사용
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

}
