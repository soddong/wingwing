package com.ssafy.shieldroneapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ssafy.shieldroneapp.data.source.local.UserLocalDataSourceImpl
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.services.connection.MobileConnectionManager
import com.ssafy.shieldroneapp.ui.authentication.AuthenticationScreen
import com.ssafy.shieldroneapp.ui.landing.LandingScreen
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.utils.Constants.Navigation.ROUTE_AUTHENTICATION
import dagger.hilt.android.AndroidEntryPoint
import com.ssafy.shieldroneapp.utils.Constants.Navigation.ROUTE_LANDING
import com.ssafy.shieldroneapp.ui.map.MapScreen
import com.ssafy.shieldroneapp.ui.map.screens.AlertHandler
import com.ssafy.shieldroneapp.utils.Constants.Navigation.ROUTE_MAP
import com.ssafy.shieldroneapp.utils.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MobileMainActivity : ComponentActivity() {

    @Inject
    lateinit var userLocalDataSource: UserLocalDataSourceImpl

    @Inject
    lateinit var connectionManager: MobileConnectionManager

    @Inject
    lateinit var alertHandler: AlertHandler

    private val _isAppActive = mutableStateOf(false)
    val isAppActive: State<Boolean> = _isAppActive

    private lateinit var messageClient: MessageClient
    private lateinit var nodeClient: NodeClient

    companion object {
        private const val TAG = "모바일: 메인 액티비티"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 초기화
        messageClient = Wearable.getMessageClient(this)
        nodeClient = Wearable.getNodeClient(this)

        // 앱이 시작될 때 모바일 앱이 활성화되었음을 워치에 알림
        lifecycleScope.launch {
            try {
                // 워치 앱 실행 시도
                val result = launchWatchAppWithRetry()
                Log.d(TAG, "워치 앱 실행 결과: ${if (result) "성공" else "실패"}")

                if (result) {
                    delay(1000)
                    connectionManager.notifyWatchOfMobileStatus(true)
                    updateAppActive(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "앱 초기화 중 오류 발생", e)
            }
        }

        setContent {
            val navController = rememberNavController()
            val coroutineScope = rememberCoroutineScope()

            // 토큰이 있는지 확인하여 초기 화면 결정
            val startDestination = if (userLocalDataSource.getTokensSync() != null) {
                ROUTE_MAP // 로그인 한 상태면(토큰 O) => Map 화면으로
            } else {
                ROUTE_LANDING // 로그인 하지 않은 상태면(토큰 X) => Landing 화면으로
            }

            ShieldroneappTheme {
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isSystemInDarkTheme()

                DisposableEffect(systemUiController, useDarkIcons) {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                    onDispose { }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination // 시작 화면 설정
                ) {
                    composable(ROUTE_LANDING) {
                        LandingScreen(onStartClick = {
                            navController.navigate(ROUTE_AUTHENTICATION) {
                          //  navController.navigate("main_screen") {
                            // navController.navigate(ROUTE_MAP) {
                                // Landing 화면은 백스택에서 제거하여 뒤로가기 방지
                                popUpTo(ROUTE_LANDING) { inclusive = true }
                            }
                        })
                    }
                    composable(ROUTE_AUTHENTICATION) {
                        AuthenticationScreen(
                            onAuthComplete = {
                                navController.navigate(ROUTE_MAP) {
                                    // 인증 화면들도 백스택에서 제거
                                    popUpTo(ROUTE_AUTHENTICATION) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(ROUTE_MAP) {
                        MapScreen(
                            isAppActive = isAppActive.value,
                            alertHandler = alertHandler,
                            coroutineScope = coroutineScope
                        )
                    }
                }
            }
        }
    }

    private suspend fun launchWatchAppWithRetry(): Boolean {
        repeat(5) { attempt ->
            if (launchWatchApp()) return true
            Log.d(TAG, "워치 앱 실행 재시도 ${attempt + 1}/5")
            delay(2000L)
        }
        return false
    }

    private suspend fun launchWatchApp(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await(5000)
            if (nodes.isEmpty()) {
                Log.e(TAG, "연결된 워치가 없습니다")
                return false
            }

            var success = false
            nodes.forEach { node ->
                try {
                    messageClient.sendMessage(
                        node.id,
                        "/start",
                        null
                    ).await(5000)
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


    override fun onResume() {
        super.onResume()
        if (!isAppActive.value) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    connectionManager.notifyWatchOfMobileStatus(true)
                    withContext(Dispatchers.Main) {
                        updateAppActive(true)
                    }
                    Log.d(TAG, "앱 활성화 상태 전송 완료")
                } catch (e: Exception) {
                    Log.e(TAG, "앱 활성화 상태 전송 실패", e)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "앱 종료 감지: isFinishing=$isFinishing")
                connectionManager.notifyWatchOfMobileStatus(false)
                updateAppActive(false)
            } catch (e: Exception) {
                Log.e(TAG, "앱 종료 상태 전송 실패", e)
            }
        }
    }

    private fun updateAppActive(isActive: Boolean) {
        _isAppActive.value = isActive
        Log.d(TAG, "앱 활성 상태 업데이트: $isActive")
    }
}