package com.ssafy.shieldroneapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ssafy.shieldroneapp.ui.authentication.AuthenticationScreen
import com.ssafy.shieldroneapp.ui.landing.LandingScreen
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.utils.Constants.Navigation.ROUTE_AUTHENTICATION
import dagger.hilt.android.AndroidEntryPoint
import com.ssafy.shieldroneapp.utils.Constants.Navigation.ROUTE_LANDING

import com.ssafy.shieldroneapp.ui.MainScreen

@AndroidEntryPoint
class MobileMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}