package com.ssafy.shieldroneapp.di

import android.app.Application
import android.content.Context
import com.ssafy.shieldroneapp.data.repository.DataRepository
import com.ssafy.shieldroneapp.data.repository.SensorRepository
import com.ssafy.shieldroneapp.services.connection.WearConnectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
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
}