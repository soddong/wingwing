package com.ssafy.shieldroneapp.ui.authentication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.request.UserAuthRequest
import com.ssafy.shieldroneapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 사용자 인증 및 회원가입 과정을 관리하는 ViewModel
 *
 * [주요 역할]
 * - Repository로부터 데이터를 받아 UI 상태를 관리하고 업데이트
 * - onSuccess와 onFailure를 통해 Repository의 요청 결과를 받아
 *   성공/실패에 따라 UI 상태를 업데이트하거나 후속 작업을 수행
 * - 사용자 입력 데이터에 대한 내부 로직을 처리하여 화면 단계를 관리
 *
 * [UI와의 데이터 흐름]
 * - 화면(Activity/Fragment)에서 ViewModel의 상태를 구독하여 필요한 데이터를 제공받음
 * - UI 로직과 분리하여 화면 상태와 이벤트를 관리하며,
 *   입력값에 따른 내부 상태 업데이트 및 화면 전환을 수행
 */
@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AuthenticationViewModel"
    }

    private val _state = MutableStateFlow(AuthenticationState())
    val state: StateFlow<AuthenticationState> = _state.asStateFlow()

    /**
     * 인증 관련 이벤트를 처리하는 함수
     * @param event AuthenticationEvent: 사용자 입력 또는 동작 이벤트
     */
    fun handleEvent(event: AuthenticationEvent) {
        when (event) {
            is AuthenticationEvent.StartAuthentication -> moveToNextStep()
            is AuthenticationEvent.NameSubmitted -> handleNameSubmission(event.name)
            is AuthenticationEvent.BirthSubmitted -> handleBirthSubmission(event.birth)
            is AuthenticationEvent.PhoneSubmitted -> handlePhoneSubmission(event.phone)
            is AuthenticationEvent.VerificationSubmitted -> handleVerificationSubmission(event.code)
            is AuthenticationEvent.TermsAccepted -> handleTermsAcceptance(event.accepted)
            is AuthenticationEvent.BackPressed -> handleBackPress()
            is AuthenticationEvent.NextPressed -> moveToNextStep()
            is AuthenticationEvent.ResendVerification -> resendVerificationCode()
        }
    }

    /**
     * 사용자 이름 제출을 처리하고 다음 단계로 이동
     * @param name 사용자 이름
     */
    private fun handleNameSubmission(name: String) {
        _state.update { it.copy(username = name, error = null) }
        moveToNextStep()
    }

    /**
     * 생년월일 제출을 처리하고 다음 단계로 이동
     * @param birth 생년월일
     */
    private fun handleBirthSubmission(birth: String) {
        _state.update { it.copy(birthday = birth, error = null) }
        moveToNextStep()
    }

    /**
     * 핸드폰 번호 제출을 처리하고 인증번호 요청
     * @param phone 핸드폰 번호
     */
    private fun handlePhoneSubmission(phone: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                userRepository.requestVerification(phone).onSuccess {
                    _state.update {
                        it.copy(
                            phoneNumber = phone,
                            isVerificationSent = true,
                            error = null
                        )
                    }
                    moveToNextStep()
                }.onFailure { error ->
                    setError(error.message ?: "인증번호 전송에 실패했습니다")
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 사용자가 입력한 인증 코드를 검증하고, 성공 여부에 따라 다음 단계로 이동
     * @param code 사용자가 입력한 인증 코드
     */
    private fun handleVerificationSubmission(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val response = userRepository.verifyCode(state.value.phoneNumber, code) // 인증 코드 검증 결과

                response.onSuccess { verificationResponse ->
                    _state.update {
                        it.copy(
                            isVerified = true,
                            isLoading = false,
                            error = null
                        )
                    }

                    if (verificationResponse.isAlreadyRegistered) {
                        handleExistingUser() // 로그인 처리
                    } else {
                        moveToNextStep() // 약관 동의 단계로 이동
                    }

                }.onFailure { error ->
                    setError(error.message ?: "인증에 실패했습니다.")
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 약관 동의 여부를 처리하고, 동의 시 회원가입 완료 진행
     * @param accepted 약관 동의 여부
     */
    private fun handleTermsAcceptance(accepted: Boolean) {
        _state.update { it.copy(isTermsAccepted = accepted) }
        if (accepted) {
            completeRegistration()
        }
    }

    /**
     * 회원가입 완료 후 로그인 절차 진행
     */
    private fun completeRegistration() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val userData = UserAuthRequest(
                    username = state.value.username,
                    birthday = state.value.birthday,
                    phoneNumber = state.value.phoneNumber,
                )
//                Log.d(TAG, userData.toString())
                Log.d(TAG, "UserData JSON: ${Gson().toJson(userData)}")
                userRepository.registerUser(userData)
                    .onSuccess {
                        Log.d(TAG, "회원가입 성공")
                        // 회원가입 성공 후 자동 로그인
                        userRepository.loginUser(userData.phoneNumber)
                            .onSuccess { tokens ->
                                Log.d(TAG, "로그인 성공: $tokens")
                                userRepository.saveTokens(tokens)
                                _state.update {
                                    it.copy(
                                        currentStep = AuthStep.Complete,
                                        isLoading = false,
                                        error = null
                                    )
                                }
                            }.onFailure { error ->
                                Log.e(TAG, "로그인 실패: ${error.message}")
                            }
                    }.onFailure { error ->
                        Log.e(TAG, "회원가입 실패: ${error.message}")
                        setError(error.message ?: "회원가입에 실패했습니다")
                    }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 기존 사용자 로그인 처리
     */
    private fun handleExistingUser() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                userRepository.loginUser(state.value.phoneNumber)
                    .onSuccess { tokens ->
                        userRepository.saveTokens(tokens)
                        _state.update {
                            it.copy(
                                currentStep = AuthStep.Complete,
                                isLoading = false,
                                error = null
                            )
                        }
                    }.onFailure { error ->
                        setError(error.message ?: "로그인에 실패했습니다")
                    }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 인증번호 재전송 요청
     */
    private fun resendVerificationCode() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                userRepository.requestVerification(state.value.phoneNumber)
                    .onSuccess {
                        _state.update {
                            it.copy(
                                isVerificationSent = true,
                                isLoading = false,
                                error = null
                            )
                        }
                    }.onFailure { error ->
                        setError(error.message ?: "인증번호 재전송에 실패했습니다")
                    }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 현재 단계에서 다음 단계로 이동
     */
    private fun moveToNextStep() {
        val nextStep = when (state.value.currentStep) {
            AuthStep.Intro -> AuthStep.Name
            AuthStep.Name -> AuthStep.Birth
            AuthStep.Birth -> AuthStep.Phone
            AuthStep.Phone -> AuthStep.Verification
            AuthStep.Verification -> AuthStep.Terms
            AuthStep.Terms -> AuthStep.Complete
            AuthStep.Complete -> return
        }
        _state.update { it.copy(currentStep = nextStep, error = null) }
    }

    /**
     * 뒤로가기 버튼을 눌렀을 때 이전 단계로 이동
     */
    private fun handleBackPress() {
        val previousStep = when (state.value.currentStep) {
            AuthStep.Intro -> return
            AuthStep.Name -> AuthStep.Intro
            AuthStep.Birth -> AuthStep.Name
            AuthStep.Phone -> AuthStep.Birth
            AuthStep.Verification -> AuthStep.Phone
            AuthStep.Terms -> AuthStep.Verification
            AuthStep.Complete -> return
        }
        _state.update { it.copy(currentStep = previousStep, error = null) }
    }

    /**
     * 오류 메시지 설정
     * @param message 오류 메시지
     */
    private fun setError(message: String) {
        _state.update { it.copy(error = message, isLoading = false) }
    }

    /**
     * 오류 메시지 초기화
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}