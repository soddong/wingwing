package com.ssafy.shieldroneapp.ui.authentication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.shieldroneapp.ui.authentication.screens.*
import kotlinx.coroutines.delay

/**
 * 사용자 인증 프로세스의 화면 전환과 상태를 관리하는 컨트롤러 Composable
 *
 * 주요 기능:
 * 1. 인증 단계별 화면 전환 제어 (인트로 -> 이름 입력 -> 생년월일 -> 전화번호 -> 인증 -> 약관)
 * 2. 각 화면에서 발생하는 이벤트를 ViewModel로 전달
 * 3. 인증 완료 시 다음 플로우로 네비게이션
 * 4. 로딩 상태 및 에러 메시지 표시
 *
 * @param onAuthComplete 인증 완료 시 다음 화면으로 이동하기 위한 콜백
 * @param viewModel 인증 프로세스의 상태 관리 및 비즈니스 로직을 처리하는 ViewModel
 */
@Composable
fun AuthenticationScreen(
    onAuthComplete: () -> Unit,
    viewModel: AuthenticationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage = state.error

    LaunchedEffect(state.currentStep) {
        if (state.currentStep == AuthStep.Complete) {
            onAuthComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 현재 단계에 따른 화면 표시
        when (state.currentStep) {
            AuthStep.Intro -> IntroScreen(
                onAuthenticateClick = {
                    viewModel.handleEvent(AuthenticationEvent.StartAuthentication)
                }
            )

            AuthStep.Name -> NameInputScreen(
                initialName = state.username,
                onNameSubmit = { name ->
                    viewModel.handleEvent(AuthenticationEvent.NameSubmitted(name))
                },
                onBackClick = {
                    viewModel.handleEvent(AuthenticationEvent.BackPressed)
                }
            )

            AuthStep.Birth -> BirthInputScreen(
                initialBirth = state.birthday,
                onBirthSubmit = { birth ->
                    viewModel.handleEvent(AuthenticationEvent.BirthSubmitted(birth))
                },
                onBackClick = {
                    viewModel.handleEvent(AuthenticationEvent.BackPressed)
                }
            )

            AuthStep.Phone -> PhoneInputScreen(
                initialPhoneNumber = state.phoneNumber,
                onPhoneSubmit = { phone ->
                    viewModel.handleEvent(AuthenticationEvent.PhoneSubmitted(phone))
                },
                onBackClick = {
                    viewModel.handleEvent(AuthenticationEvent.BackPressed)
                }
            )

            AuthStep.Verification -> VerificationScreen(
                onVerificationSubmit = { code ->
                    viewModel.handleEvent(AuthenticationEvent.VerificationSubmitted(code))
                },
                onResendCode = {
                    viewModel.handleEvent(AuthenticationEvent.ResendVerification)
                },
                onBackClick = {
                    viewModel.handleEvent(AuthenticationEvent.BackPressed)
                }
            )

            AuthStep.Terms -> TermAgreementScreen(
                onAccept = {
                    viewModel.handleEvent(AuthenticationEvent.TermsAccepted(true))
                },
                onBackClick = {
                    viewModel.handleEvent(AuthenticationEvent.BackPressed)
                }
            )

            AuthStep.Complete -> {
                // 완료 화면은 LaunchedEffect에서 처리
            }
        }

        // 로딩 표시
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colors.primary
            )
        }

        // 에러 메시지 표시
        if (errorMessage != null) {
            LaunchedEffect(errorMessage) {
                delay(3000) // 3초 후에 Snackbar가 사라짐
                viewModel.clearError() // 에러 메시지 초기화 함수 호출
            }
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text(errorMessage)
            }
        }

    }
}

