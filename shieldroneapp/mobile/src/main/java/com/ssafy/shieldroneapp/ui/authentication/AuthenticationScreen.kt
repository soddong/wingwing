package com.ssafy.shieldroneapp.ui.authentication

/**
 * 전체 인증 프로세스를 관리하는 화면.
 *
 * 단계별 화면을 순서대로 호출하며, 사용자가 입력한 데이터를 ViewModel에서 관리한다.
 * `currentStep` 상태에 따라 각 단계 컴포저블을 표시하며,
 * ViewModel을 통해 단계 전환과 데이터 처리를 중앙에서 관리한다.
 *
 * @property viewModel 인증 과정을 관리하는 ViewModel (필요할 경우 추가)
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.shieldroneapp.ui.authentication.screens.IntroScreen

/**
 * 인증 단계를 정의하는 sealed class
 */
sealed class AuthStep {
    object Intro : AuthStep()
//    object Name : AuthStep()
//    object ResidentNumber : AuthStep()
//    object PhoneNumber : AuthStep()
//    object Verification : AuthStep()
//    object Terms : AuthStep()
    object Complete : AuthStep()
}

@Composable
fun AuthenticationScreen(
    onAuthComplete: () -> Unit,
    viewModel: AuthenticationViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    when (currentStep) {
        AuthStep.Intro -> {
            IntroScreen(
                onAuthenticateClick = { viewModel.moveToNextStep() }
            )
        }

//        AuthStep.Name -> {
//            NameInputScreen(
//                onNameSubmit = { name ->
//                    viewModel.setName(name)
//                    viewModel.moveToNextStep()
//                },
//                onBackClick = { viewModel.moveToPreviousStep() }
//            )
//        }

//        AuthStep.ResidentNumber -> {
//            ResidentNumberScreen(
//                onSubmit = { residentNumber ->
//                    viewModel.setResidentNumber(residentNumber)
//                    viewModel.moveToNextStep()
//                },
//                onBackClick = { viewModel.moveToPreviousStep() }
//            )
//        }

//        AuthStep.PhoneNumber -> {
//            PhoneNumberScreen(
//                onSubmit = { phoneNumber ->
//                    viewModel.setPhoneNumber(phoneNumber)
//                    viewModel.requestVerification()
//                },
//                onBackClick = { viewModel.moveToPreviousStep() }
//            )
//        }

//        AuthStep.Verification -> {
//            VerificationScreen(
//                onVerificationSubmit = { code ->
//                    viewModel.verifyCode(code)
//                },
//                onResendCode = { viewModel.resendVerificationCode() },
//                onBackClick = { viewModel.moveToPreviousStep() }
//            )
//        }

//        AuthStep.Terms -> {
//            TermsScreen(
//                onAccept = {
//                    viewModel.acceptTerms()
//                    viewModel.registerUser()
//                },
//                onBackClick = { viewModel.moveToPreviousStep() }
//            )
//        }

        AuthStep.Complete -> {
            onAuthComplete()
        }
    }
}