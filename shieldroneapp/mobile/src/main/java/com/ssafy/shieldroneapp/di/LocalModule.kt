package com.ssafy.shieldroneapp.di

import com.ssafy.shieldroneapp.data.source.local.DroneLocalDataSource
import com.ssafy.shieldroneapp.data.source.local.DroneLocalDataSourceImpl
import com.ssafy.shieldroneapp.data.source.local.MapLocalDataSource
import com.ssafy.shieldroneapp.data.source.local.MapLocalDataSourceImpl
import com.ssafy.shieldroneapp.data.source.local.UserLocalDataSource
import com.ssafy.shieldroneapp.data.source.local.UserLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 로컬 데이터 소스 주입을 위한 Hilt 모듈
 *
 * [주요 역할]
 * - LocalDataSource 인터페이스의 구현체로 LocalDataSourceImpl을 제공
 * - 앱 전역에서 하나의 LocalDataSource 인스턴스를 사용하도록 Singleton 범위로 설정
 *
 * [설정 내용]
 * - @Binds: LocalDataSourceImpl을 LocalDataSource 타입으로 제공
 * - @InstallIn(SingletonComponent::class): 앱 전역(SingletonComponent)에서 주입 가능하도록 모듈 설치
 * - @Singleton: LocalDataSource 인스턴스를 싱글톤으로 관리하여 메모리 효율성을 높임
 * - @ApplicationContext: Context 인스턴스를 Hilt에서 제공받아 LocalDataSourceImpl에 주입
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocalModule {
    @Binds
    @Singleton
    abstract fun bindUserLocalDataSource(
        userLocalDataSourceImpl: UserLocalDataSourceImpl
    ): UserLocalDataSource

    @Binds
    @Singleton
    abstract fun bindMapLocalDataSource(
        mapLocalDataSourceImpl: MapLocalDataSourceImpl
    ): MapLocalDataSource

    @Binds
    @Singleton
    abstract fun bindDroneLocalDataSource(
        droneLocalDataSourceImpl: DroneLocalDataSourceImpl
    ): DroneLocalDataSource

}