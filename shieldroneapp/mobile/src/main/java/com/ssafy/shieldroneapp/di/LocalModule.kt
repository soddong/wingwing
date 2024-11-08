package com.ssafy.shieldroneapp.di

import android.content.Context
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.repository.UserRepository
import com.ssafy.shieldroneapp.data.repository.UserRepositoryImpl
import com.ssafy.shieldroneapp.data.source.local.UserLocalDataSource
import com.ssafy.shieldroneapp.data.source.local.UserLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 로컬 데이터 소스 주입을 위한 Hilt 모듈
 *
 * [주요 역할]
 * - UserLocalDataSource 인터페이스의 구현체로 UserLocalDataSourceImpl을 제공
 * - 앱 전역에서 하나의 UserLocalDataSource 인스턴스를 사용하도록 Singleton 범위로 설정
 *
 * [설정 내용]
 * - @Provides: UserLocalDataSourceImpl을 UserLocalDataSource 타입으로 제공
 * - @InstallIn(SingletonComponent::class): 앱 전역(SingletonComponent)에서 주입 가능하도록 모듈 설치
 * - @Singleton: UserLocalDataSource 인스턴스를 싱글톤으로 관리하여 메모리 효율성을 높임
 * - @ApplicationContext: Context 인스턴스를 Hilt에서 제공받아 UserLocalDataSourceImpl에 주입
 */
@Module
@InstallIn(SingletonComponent::class)
object LocalModule {
    @Provides
    @Singleton
    fun provideUserLocalDataSource(
        @ApplicationContext context: Context,
        gson: Gson
    ): UserLocalDataSource {
        return UserLocalDataSourceImpl(context, gson)
    }
}