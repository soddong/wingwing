package com.ssafy.shieldroneapp.ui.authentication

/**
 * 인증 과정의 상태와 데이터를 관리하는 ViewModel.
 *
 * 각 인증 단계의 상태와 사용자 입력 데이터를 일시적으로 관리하고,
 * 단계 이동 함수 `goToNextStep`으로 인증 흐름을 제어한다.
 *
 * 단계별 입력 데이터는 `mutableStateOf`를 사용해 ViewModel 내에서 임시로 저장되며,
 * 모든 인증 과정이 완료된 후에는 `UserRepository`를 통해 최종적으로
 * 로컬 저장소 또는 서버로 데이터를 전송하여 저장할 수 있다.
 *
 * @property userRepository 사용자 데이터를 저장하고 전송하는 리포지토리 객체
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
//    private val userRepository: UserRepository
) : ViewModel() {

    // 현재 인증 단계를 관리하는 상태
    private val _currentStep = MutableStateFlow<AuthStep>(AuthStep.Intro)
    val currentStep: StateFlow<AuthStep> = _currentStep.asStateFlow()

    // UI 상태 관리 (로딩, 에러 등)
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // 다음 단계로 이동
    fun moveToNextStep() {
        _currentStep.value = AuthStep.Complete  // 지금은 바로 Complete로 이동
    }

    // 이전 단계로 이동
    fun moveToPreviousStep() {
        _currentStep.value = AuthStep.Intro
    }


//    private var userData = UserAuthData()

    // 다음 단계로 이동
//    fun moveToNextStep() {
//        _currentStep.value = when (_currentStep.value) {
//            AuthStep.Intro -> AuthStep.Name
//            AuthStep.Name -> AuthStep.ResidentNumber
//            AuthStep.ResidentNumber -> AuthStep.PhoneNumber
//            AuthStep.PhoneNumber -> AuthStep.Verification
//            AuthStep.Verification -> {
//                if (userRepository.isUserRegistered(userData.phoneNumber)) {
//                    AuthStep.Complete // 기존 회원은 약관 동의 없이 완료
//                } else {
//                    AuthStep.Terms // 신규 회원은 약관 동의로
//                }
//            }
//            AuthStep.Terms -> AuthStep.Complete
//            AuthStep.Complete -> AuthStep.Complete
//        }
//    }

//    fun moveToPreviousStep() {
//        _currentStep.value = when (_currentStep.value) {
//            AuthStep.Intro -> AuthStep.Intro
//            AuthStep.Name -> AuthStep.Intro
//            AuthStep.ResidentNumber -> AuthStep.Name
//            AuthStep.PhoneNumber -> AuthStep.ResidentNumber
//            AuthStep.Verification -> AuthStep.PhoneNumber
//            AuthStep.Terms -> AuthStep.Verification
//            AuthStep.Complete -> AuthStep.Complete
//        }
//    }

//    fun setName(name: String) {
//        userData = userData.copy(name = name)
//    }
//
//    fun setResidentNumber(number: String) {
//        userData = userData.copy(residentNumber = number)
//    }
//
//    fun setPhoneNumber(number: String) {
//        userData = userData.copy(phoneNumber = number)
//    }

//    fun requestVerification() {
//        viewModelScope.launch {
//            try {
//                _uiState.value = _uiState.value.copy(isLoading = true)
//                userRepository.requestVerification(userData.phoneNumber)
//                moveToNextStep()
//            } catch (e: Exception) {
//                _uiState.value = _uiState.value.copy(error = e.message)
//            } finally {
//                _uiState.value = _uiState.value.copy(isLoading = false)
//            }
//        }
//    }

//    fun verifyCode(code: String) {
//        viewModelScope.launch {
//            try {
//                _uiState.value = _uiState.value.copy(isLoading = true)
//                val isVerified = userRepository.verifyCode(userData.phoneNumber, code)
//                if (isVerified) {
//                    moveToNextStep()
//                } else {
//                    _uiState.value = _uiState.value.copy(error = "인증번호가 일치하지 않습니다")
//                }
//            } catch (e: Exception) {
//                _uiState.value = _uiState.value.copy(error = e.message)
//            } finally {
//                _uiState.value = _uiState.value.copy(isLoading = false)
//            }
//        }
//    }

//    fun registerUser() {
//        viewModelScope.launch {
//            try {
//                _uiState.value = _uiState.value.copy(isLoading = true)
//                userRepository.register(userData)
//                // 회원가입 후 자동 로그인
//                val tokens = userRepository.login(userData.phoneNumber)
//                saveTokens(tokens)
//                moveToNextStep()
//            } catch (e: Exception) {
//                _uiState.value = _uiState.value.copy(error = e.message)
//            } finally {
//                _uiState.value = _uiState.value.copy(isLoading = false)
//            }
//        }
//    }

//    private fun saveTokens(tokens: Tokens) {
//        // DataStore나 SharedPreferences에 토큰 저장
//        viewModelScope.launch {
//            userRepository.saveTokens(tokens)
//        }
//    }
}

//data class UserAuthData(
//    val name: String = "",
//    val residentNumber: String = "",
//    val phoneNumber: String = "",
//    val isTermsAccepted: Boolean = false
//)

// UI 상태를 나타내는 데이터 클래스
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

//data class Tokens(
//    val accessToken: String,
//    val refreshToken: String
//)