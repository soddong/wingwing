package com.ssafy.shieldroneapp.di

import android.app.Application
import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.domain.repository.DataRepository
import com.ssafy.shieldroneapp.domain.repository.SensorRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearableModule {
    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideSensorRepository(
        context: Context
    ): SensorRepository {
        return SensorRepository(context)
    }

    @Provides
    @Singleton
    fun provideDataRepository(
        context: Context
    ): DataRepository {
        return DataRepository(context)
    }

    @Provides
    @Singleton
    fun provideMessageClient(@ApplicationContext context: Context): MessageClient {
        return Wearable.getMessageClient(context)
    }
}