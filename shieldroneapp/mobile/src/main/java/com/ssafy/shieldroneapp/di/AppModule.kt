package com.ssafy.shieldroneapp.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ssafy.shieldroneapp.data.audio.AudioAnalyzer
import com.ssafy.shieldroneapp.data.audio.AudioRecorder
import com.ssafy.shieldroneapp.data.repository.AudioDataRepository
import com.ssafy.shieldroneapp.data.repository.HeartRateDataRepository
import com.ssafy.shieldroneapp.data.source.local.AudioDataLocalSource
import com.ssafy.shieldroneapp.data.source.local.HeartRateLocalDataSource
import com.ssafy.shieldroneapp.data.source.remote.WebSocketConnectionManager
import com.ssafy.shieldroneapp.data.source.remote.WebSocketErrorHandler
import com.ssafy.shieldroneapp.data.source.remote.WebSocketMessageSender
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import com.ssafy.shieldroneapp.data.source.remote.WebSocketSubscriptions
import com.ssafy.shieldroneapp.permissions.PermissionManager
import com.ssafy.shieldroneapp.services.connection.MobileConnectionManager
import com.ssafy.shieldroneapp.viewmodels.HeartRateViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideContext(
        @ApplicationContext context: Context
    ): Context {
        return context
    }

    @Provides
    @Singleton
    fun providePermissionManager(
        @ApplicationContext context: Context
    ): PermissionManager {
        return PermissionManager(context)
    }

    @Provides
    @Singleton
    fun provideWebSocketErrorHandler(
        @ApplicationContext context: Context
    ): WebSocketErrorHandler {
        return WebSocketErrorHandler(context)
    }

    @Provides
    @Singleton
    fun provideWebSocketMessageSender(): WebSocketMessageSender {
        return WebSocketMessageSender(null)
    }

    @Provides
    @Singleton
    fun provideWebSocketConnectionManager(
        webSocketService: WebSocketService,
        webSocketMessageSender: WebSocketMessageSender,
        errorHandler: WebSocketErrorHandler,
        webSocketSubscriptions: WebSocketSubscriptions
    ): WebSocketConnectionManager {
        return WebSocketConnectionManager(
            webSocketService,
            webSocketMessageSender,
            errorHandler,
            webSocketSubscriptions
        )
    }

    @Provides
    @Singleton
    fun provideWebSocketService(
        context: Context,
        webSocketMessageSender: WebSocketMessageSender,
        audioDataLocalSource: AudioDataLocalSource,
        webSocketSubscriptions: WebSocketSubscriptions
    ): WebSocketService {
        val service = WebSocketService(context, webSocketMessageSender, audioDataLocalSource)
        val connectionManager = provideWebSocketConnectionManager(
            service,
            webSocketMessageSender,
            provideWebSocketErrorHandler(context),
            webSocketSubscriptions
        )
        service.setConnectionManager(connectionManager)
        return service
    }

    @Provides
    @Singleton
    fun provideAudioAnalyzer(): AudioAnalyzer {
        return AudioAnalyzer()
    }

    @Provides
    @Singleton
    fun provideAudioRecorder(
        @ApplicationContext context: Context,
        audioDataRepository: AudioDataRepository,
        webSocketService: WebSocketService,
        audioAnalyzer: AudioAnalyzer
    ): AudioRecorder {
        return AudioRecorder(
            context,
            audioDataRepository,
            webSocketService,
            audioAnalyzer
        )
    }

    @Provides
    @Singleton
    fun provideAudioDataLocalSource(
        @ApplicationContext context: Context
    ): AudioDataLocalSource {
        return AudioDataLocalSource(context)
    }

    @Provides
    @Singleton
    fun provideAudioDataRepository(
        webSocketService: WebSocketService,
        audioDataLocalSource: AudioDataLocalSource
    ): AudioDataRepository {
        return AudioDataRepository(webSocketService, audioDataLocalSource)
    }

    @Provides
    @Singleton
    fun provideSensorDataRepository(
        webSocketService: WebSocketService,
        heartRateViewModel: HeartRateViewModel,
        heartRateLocalDataSource: HeartRateLocalDataSource,

        ): HeartRateDataRepository {
        return HeartRateDataRepository(
            webSocketService,
            heartRateViewModel,
            heartRateLocalDataSource
        )
    }

    @Provides
    fun provideHeartRateViewModel(
        connectionManager: MobileConnectionManager
    ): HeartRateViewModel {
        return HeartRateViewModel(connectionManager)
    }

    @Provides
    @Singleton
    fun provideMobileConnectionManager(
        @ApplicationContext context: Context
    ): MobileConnectionManager {
        return MobileConnectionManager(context)
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
}