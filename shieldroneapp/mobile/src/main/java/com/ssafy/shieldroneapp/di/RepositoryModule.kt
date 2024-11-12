package com.ssafy.shieldroneapp.di

import com.ssafy.shieldroneapp.data.repository.DroneRepository
import com.ssafy.shieldroneapp.data.repository.DroneRepositoryImpl
import com.ssafy.shieldroneapp.data.repository.MapRepository
import com.ssafy.shieldroneapp.data.repository.MapRepositoryImpl
import com.ssafy.shieldroneapp.data.repository.UserRepository
import com.ssafy.shieldroneapp.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 주입을 위한 Hilt 모듈
 *
 * [주요 역할]
 * - Repository 인터페이스의 구현체로 RepositoryImpl을 제공
 * - 앱 전역에서 하나의 Repository 인스턴스를 사용하도록 Singleton 범위로 설정
 *
 * [설정 내용]
 * - @Binds: RepositoryImpl을 Repository 타입으로 주입할 수 있도록 설정
 * - @InstallIn(SingletonComponent::class): 앱 전역(SingletonComponent)에서 주입 가능하도록 모듈 설치
 * - @Singleton: Repository 인스턴스를 싱글톤으로 관리하여 메모리 효율성을 높임
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindMapRepository(
        mapRepositoryImpl: MapRepositoryImpl
    ): MapRepository

    @Binds
    @Singleton
    abstract fun bindDroneRepository(
        droneRepositoryImpl: DroneRepositoryImpl
    ): DroneRepository

}