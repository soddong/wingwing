package com.ssafy.shieldroneapp.di

import android.content.Context
import com.ssafy.shieldroneapp.data.audio.AudioAnalyzer
import com.ssafy.shieldroneapp.data.audio.AudioRecorder
import com.ssafy.shieldroneapp.data.repository.HeartRateDataRepository
import com.ssafy.shieldroneapp.data.source.local.AudioDataLocalSource
import com.ssafy.shieldroneapp.data.source.local.HeartRateLocalDataSource
import com.ssafy.shieldroneapp.data.source.remote.WebSocketConnectionManager
import com.ssafy.shieldroneapp.data.source.remote.WebSocketErrorHandler
import com.ssafy.shieldroneapp.data.source.remote.WebSocketMessageSender
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
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
        errorHandler: WebSocketErrorHandler
    ): WebSocketConnectionManager {
        return WebSocketConnectionManager(
            webSocketService = webSocketService,
            webSocketMessageSender = webSocketMessageSender,
            errorHandler = errorHandler
        )
    }

    @Provides
    @Singleton
    fun provideWebSocketService(
        context: Context,
        webSocketMessageSender: WebSocketMessageSender,
        audioDataLocalSource: AudioDataLocalSource
    ): WebSocketService {
        val service = WebSocketService(context, webSocketMessageSender, audioDataLocalSource)
        val connectionManager = provideWebSocketConnectionManager(
            service,
            webSocketMessageSender,
            provideWebSocketErrorHandler(context)
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
        webSocketService: WebSocketService,
        audioAnalyzer: AudioAnalyzer
    ): AudioRecorder {
        return AudioRecorder(context, webSocketService, audioAnalyzer)
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
    @Singleton
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
}