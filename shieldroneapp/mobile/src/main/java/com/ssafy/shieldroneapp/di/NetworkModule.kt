package com.ssafy.shieldroneapp.di

import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.source.remote.ApiConstants
import com.ssafy.shieldroneapp.data.source.remote.ApiInterceptor
import com.ssafy.shieldroneapp.data.source.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.ssafy.shieldroneapp.BuildConfig
import com.ssafy.shieldroneapp.data.model.request.TokenRequest
import com.ssafy.shieldroneapp.data.model.response.TokenResponse
import com.ssafy.shieldroneapp.data.source.local.UserLocalDataSourceImpl
import com.ssafy.shieldroneapp.data.source.remote.TokenRefresher
import retrofit2.Response
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /**
     * Gson 인스턴스 제공
     * 오류 응답 파싱 및 데이터 변환에 사용됨
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    /**
     * HttpLoggingInterceptor 설정
     * 디버그 빌드 시 네트워크 요청/응답을 로깅하여 디버깅에 도움을 줌
     */
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * 토큰 갱신용 별도 OkHttpClient 제공
     * 토큰 갱신 요청 시 독립적으로 사용되며, 기본 네트워크 클라이언트와 분리
     */
    @Named("tokenRefreshClient")
    @Provides
    @Singleton
    fun provideTokenRefreshOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(ApiConstants.TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 토큰 갱신용 별도 Retrofit 인스턴스 제공
     * 토큰 갱신 시에만 사용하는 별도의 Retrofit 설정
     */
    @Named("tokenRefreshRetrofit")
    @Provides
    @Singleton
    fun provideTokenRefreshRetrofit(
        @Named("tokenRefreshClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 토큰 갱신용 별도 ApiService 제공
     * Retrofit을 사용해 토큰 갱신을 위한 ApiService 구현체 반환
     */
    @Named("tokenRefreshService")
    @Provides
    @Singleton
    fun provideTokenRefreshApiService(
        @Named("tokenRefreshRetrofit") retrofit: Retrofit
    ): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    /**
     * ApiInterceptor 제공
     * ApiInterceptor는 userLocalDataSource와 tokenRefresher, gson을 주입받아
     * 네트워크 요청과 응답을 처리하고, 필요한 경우 토큰을 갱신
     */
    @Provides
    @Singleton
    fun provideApiInterceptor(
        userLocalDataSource: UserLocalDataSourceImpl,
        @Named("tokenRefreshService") tokenRefreshApiService: ApiService,
        gson: Gson // Gson 인스턴스 주입
    ): ApiInterceptor {
        return ApiInterceptor(
            userLocalDataSource,
            object : TokenRefresher {
                override suspend fun refreshToken(refreshToken: String): Response<TokenResponse> {
                    return tokenRefreshApiService.refreshToken(TokenRequest(refreshToken))
                }
            },
            gson
        )
    }

    /**
     * 메인 OkHttpClient 제공
     * 메인 네트워크 요청을 처리하며, ApiInterceptor와 로깅 인터셉터를 포함
     */
    @Named("mainClient")
    @Provides
    @Singleton
    fun provideMainOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        apiInterceptor: ApiInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(apiInterceptor)
            .connectTimeout(ApiConstants.TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 메인 Retrofit 인스턴스 제공
     * API 기본 URL과 mainClient를 사용하여 설정됨
     */
    @Named("mainRetrofit")
    @Provides
    @Singleton
    fun provideMainRetrofit(
        @Named("mainClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * ApiService 생성
     * 메인 Retrofit을 사용하여 ApiService 구현체 반환
     */
    @Provides
    @Singleton
    fun provideApiService(
        @Named("mainRetrofit") retrofit: Retrofit
    ): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}