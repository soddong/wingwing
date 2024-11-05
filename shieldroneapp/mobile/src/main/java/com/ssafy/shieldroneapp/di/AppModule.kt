package com.ssafy.shieldroneapp.di

import android.content.Context
import com.ssafy.shieldroneapp.data.audio.AudioRecorder
import com.ssafy.shieldroneapp.data.source.remote.WebSocketConnectionManager
import com.ssafy.shieldroneapp.data.source.remote.WebSocketErrorHandler
import com.ssafy.shieldroneapp.data.source.remote.WebSocketMessageSender
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import com.ssafy.shieldroneapp.permissions.PermissionManager
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
    fun provideWebSocketService(
        @ApplicationContext context: Context,
        messageSender: WebSocketMessageSender,
        errorHandler: WebSocketErrorHandler
    ): WebSocketService {
        val service = WebSocketService(context, messageSender)
        val connectionManager = WebSocketConnectionManager(service, errorHandler)
        service.setConnectionManager(connectionManager)
        return service
    }

    @Provides
    @Singleton
    fun provideAudioRecorder(
        @ApplicationContext context: Context,
        webSocketService: WebSocketService
    ): AudioRecorder {
        return AudioRecorder(context, webSocketService)
    }
}