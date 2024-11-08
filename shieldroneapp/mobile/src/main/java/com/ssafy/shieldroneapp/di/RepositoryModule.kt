package com.ssafy.shieldroneapp.di

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
 * - UserRepository 인터페이스의 구현체로 UserRepositoryImpl을 제공
 * - 앱 전역에서 하나의 UserRepository 인스턴스를 사용하도록 Singleton 범위로 설정
 *
 * [설정 내용]
 * - @Binds: UserRepositoryImpl을 UserRepository 타입으로 주입할 수 있도록 설정
 * - @InstallIn(SingletonComponent::class): 앱 전역(SingletonComponent)에서 주입 가능하도록 모듈 설치
 * - @Singleton: UserRepository 인스턴스를 싱글톤으로 관리하여 메모리 효율성을 높임
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
}