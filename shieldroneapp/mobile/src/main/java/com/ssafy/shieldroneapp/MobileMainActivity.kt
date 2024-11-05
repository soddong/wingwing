package com.ssafy.shieldroneapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ssafy.shieldroneapp.ui.authentication.AuthenticationScreen
import com.ssafy.shieldroneapp.ui.landing.LandingScreen
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // Hilt 사용 위해 추가
class MobileMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController() // NavController 생성

            ShieldroneappTheme {
                NavHost(
                    navController = navController,
                    startDestination = "landing_screen" // 시작 화면 설정
                ) {
                    composable("landing_screen") {
                        LandingScreen(onStartClick =  {
                            navController.navigate("authentication")
//                            {
//                                 Landing 화면은 백스택에서 제거
//                                popUpTo("landing_screen") { inclusive = true }
//                            }
                        })
                    }
                    composable("authentication") {
                        AuthenticationScreen(
                            onAuthComplete = {
                                // 인증 성공 후 메인 화면으로 이동 (지금은 임의로 landing screen)
                                navController.navigate("landing screen") {
                                    // 인증 화면들도 백스택에서 제거
                                    // popUpTo("authentication") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}