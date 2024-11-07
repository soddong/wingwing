package com.ssafy.shieldroneapp

/**
 * 앱이 실행될 때 가장 먼저 생성되는 클래스
 *
 * @HiltAndroidApp 어노테이션: Hilt의 의존성 주입 컨테이너를 생성
 * 앱 전역에서 사용할 초기 설정이나 싱글톤 객체들을 관리합니다.
 * */

import android.app.Application
import android.content.Intent
import android.util.Log
import com.ssafy.shieldroneapp.services.connection.WearableDataListenerService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MobileMainApplication : Application() {
    companion object {
        private const val TAG = "모바일: 메인 앱"
    }

    override fun onCreate() {
        super.onCreate()
        // 앱 시작시 필요한 초기화 작업
        // - 로깅 설정
        // - SharedPreferences 초기화
        // - 네트워크 설정
        // - 푸시 알림 설정 등
        setupLogging()
        // 서비스 자동 시작
        startService(Intent(this, WearableDataListenerService::class.java))
    }
    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "디버그 빌드에서만 상세 로깅 활성화")
        }
    }
}