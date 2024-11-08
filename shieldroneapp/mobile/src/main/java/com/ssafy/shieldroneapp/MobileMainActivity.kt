package com.ssafy.shieldroneapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.ui.authentication.AuthenticationScreen
import com.ssafy.shieldroneapp.ui.landing.LandingScreen
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.utils.Constants.Navigation.ROUTE_AUTHENTICATION
import dagger.hilt.android.AndroidEntryPoint
import com.ssafy.shieldroneapp.utils.Constants.Navigation.ROUTE_LANDING
import com.ssafy.shieldroneapp.ui.MainScreen
import com.ssafy.shieldroneapp.utils.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MobileMainActivity : ComponentActivity() {
    private lateinit var messageClient: MessageClient
    private lateinit var nodeClient: NodeClient

    companion object {
        private const val TAG = "모바일: 메인액티비티"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wearable 클라이언트 초기화 및 워치 앱 실행 시도
        messageClient = Wearable.getMessageClient(this)
        nodeClient = Wearable.getNodeClient(this)
        launchWatchAppWithRetry()

        setContent {
            val navController = rememberNavController()

            ShieldroneappTheme {
                // 상태바 스타일링
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isSystemInDarkTheme() // 다크 모드 여부에 따라 아이콘 색상 결정

                DisposableEffect(systemUiController, useDarkIcons) {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent, // 상태바 배경을 투명하게
                        darkIcons = useDarkIcons // 상태바 아이콘 true면 검정, false는 흰색
                    )
                    onDispose { } // 화면이 dispose될 때 정리
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_LANDING // 시작 화면 설정
                ) {
                    composable(ROUTE_LANDING) {
                        LandingScreen(onStartClick =  {
                            navController.navigate(ROUTE_AUTHENTICATION) {
                                // Landing 화면은 백스택에서 제거하여 뒤로가기 방지
                                popUpTo(ROUTE_LANDING) { inclusive = true }
                            }
                        })
                    }
                    composable(ROUTE_AUTHENTICATION) {
                        AuthenticationScreen(
                                onAuthComplete = {
                                    // 추후 메인 화면으로 이동할 때를 위한 주석
                                    // navController.navigate(ROUTE_MAIN) {
                                    //     popUpTo(ROUTE_AUTHENTICATION) { inclusive = true }
                                    // }

                                    // 임시로 landing screen
                                    navController.navigate("main_screen") {
                                        // 인증 화면들도 백스택에서 제거
                                         popUpTo(ROUTE_AUTHENTICATION) { inclusive = true }
                                    }
                                }
                            )
                    }

                    composable("main_screen") {
                        MainScreen()
                    }
                }
            }
        }
    }

    private fun launchWatchAppWithRetry() {
        lifecycleScope.launch {
            repeat(3) { attempt ->
                if (launchWatchApp()) return@launch
                delay(1000L * (attempt + 1))
            }
        }
    }

    private suspend fun launchWatchApp(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()

            if (nodes.isEmpty()) {
                Log.d(TAG, "연결된 워치가 없습니다")
                return false
            }

            var success = false
            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        "/start",
                        null
                    ).await()
                    success = true
                    Log.d(TAG, "워치 앱 실행 메시지 전송 성공: ${node.displayName}")
                } catch (e: Exception) {
                    Log.e(TAG, "워치 앱 실행 메시지 전송 실패: ${node.displayName}", e)
                }
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "워치 앱 실행 시도 중 오류", e)
            false
        }
    }
}