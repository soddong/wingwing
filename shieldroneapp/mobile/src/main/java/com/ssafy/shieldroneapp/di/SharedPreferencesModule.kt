package com.ssafy.shieldroneapp.di

import android.content.Context
import android.content.SharedPreferences
import com.ssafy.shieldroneapp.utils.SecurityUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedPreferencesModule {
    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return SecurityUtils.getEncryptedSharedPreferences(context, "wingwing_encrypted_prefs")
    }
}
