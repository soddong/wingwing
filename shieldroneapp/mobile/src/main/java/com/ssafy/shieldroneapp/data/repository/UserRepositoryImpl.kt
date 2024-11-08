package com.ssafy.shieldroneapp.data.repository

import android.content.Context
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.Guardian
import com.ssafy.shieldroneapp.data.model.request.CodeVerificationRequest
import com.ssafy.shieldroneapp.data.model.request.PhoneNumberRequest
import com.ssafy.shieldroneapp.data.model.request.TokenRequest
import com.ssafy.shieldroneapp.data.model.request.UserAuthRequest
import com.ssafy.shieldroneapp.data.model.request.VerificationCodeRequest
import com.ssafy.shieldroneapp.data.model.response.AuthenticationErrorResponse
import com.ssafy.shieldroneapp.data.model.response.TokenResponse
import com.ssafy.shieldroneapp.data.model.response.VerificationResponse
import com.ssafy.shieldroneapp.data.source.local.UserLocalDataSource
import com.ssafy.shieldroneapp.data.source.remote.ApiService
import com.ssafy.shieldroneapp.utils.NetworkUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userLocalDataSource: UserLocalDataSource,
    private val context: Context, // Context 주입 필요
    private val gson: Gson // Gson 주입 추가
) : UserRepository {

    /**
     * 공통 메서드: 네트워크 상태 확인 후 API 호출 결과를 Result로 감싸 반환하는 공통 메서드
     *
     * [Result.success]와 [Result.failure]를 사용하여 API 호출 결과를
     * Result로 감싸기 때문에 호출하는 측에서는 별도로 Result로 감쌀 필요가 없습니다.
     *
     * @param block 네트워크가 연결된 경우 실행할 API 요청 블록
     * @return API 요청 결과를 Result로 감싼 값. 네트워크 오류나 예외 발생 시 Result.failure 반환
     */
    private suspend fun <T> apiCallAfterNetworkCheck(block: suspend () -> T): Result<T> {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                Result.success(block())
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("네트워크 연결이 없습니다.")) // 네트워크 미연결 시 에러 반환
        }
    }

    /**
     * 1. 핸드폰 인증번호 요청
     *
     * @param phoneNumber 인증을 요청할 전화번호
     * @return 성공 시 Unit, 실패 시 에러와 함께 실패 결과 반환
     */
    override suspend fun requestVerification(phoneNumber: String): Result<Unit> {
        val requestBody = VerificationCodeRequest(phoneNumber)
        return apiCallAfterNetworkCheck {
            apiService.sendVerificationCode(requestBody)
        }
    }

    /**
     * 2. 인증번호 검증
     *
     * @param phoneNumber 인증을 요청한 전화번호
     * @param code 사용자가 입력한 인증번호
     * @return 인증 성공 시 `VerificationResponse` 객체를 포함한 성공 결과 반환.
     *         이미 등록된 회원 여부 정보를 포함하며, 실패 시 에러 메시지와 함께 실패 결과 반환.
     */
    override suspend fun verifyCode(phoneNumber: String, code: String): Result<VerificationResponse> {
        val requestBody = CodeVerificationRequest(phoneNumber, code)

        return apiCallAfterNetworkCheck {
            val response = apiService.verifyCode(requestBody)

            // 응답이 성공적인 경우 VerificationResponse를 Result.success로 반환
            // isSuccessful: HTTP 응답 코드가 200번대일 때 true 반환, 그 외에는 false
            if (response.isSuccessful) {
                response.body() ?: throw Exception("서버 응답이 비어 있습니다. 다시 시도해주세요.")
            } else {
                // 실패한 경우, errorBody를 VerificationErrorResponse로 변환(Gson 이용)하여 실패 처리
                val errorResponse = response.errorBody()?.string()?.let {
                    gson.fromJson(it, AuthenticationErrorResponse::class.java)
                }
                throw Exception(errorResponse?.message ?: "인증 실패")
            }
        }
    }

    /**
     * 3. 신규 사용자 등록
     * 서버에 사용자 정보 등록 후 로컬에도 저장
     *
     * @param userData 사용자 인증 과정에서 수집된 데이터
     * @return 생성된 User 객체, 실패 시 에러와 함께 실패 결과 반환
     */
    override suspend fun registerUser(userData: UserAuthRequest): Result<Unit> {
        return apiCallAfterNetworkCheck {
            val response = apiService.signUp(userData)

            if (response.isSuccessful) {
                Unit
            } else {
                val errorMessage = response.errorBody()?.string() ?: "회원가입에 실패했습니다."
                throw Exception(errorMessage)
            }
        }
    }

    /**
     * 4. 사용자 로그인 처리
     * 성공 시 액세스 토큰과 리프레시 토큰 반환
     *
     * @param phoneNumber 로그인할 전화번호
     * @return 인증 토큰, 실패 시 에러와 함께 실패 결과 반환
     */
    override suspend fun loginUser(phoneNumber: String): Result<TokenResponse> {
        return apiCallAfterNetworkCheck {
            val response = apiService.signIn(PhoneNumberRequest(phoneNumber))

            if (response.isSuccessful) {
                val tokens = response.body() ?: throw Exception("토큰 응답이 비어 있습니다.")
                userLocalDataSource.saveTokens(tokens)
                tokens
            } else {
                val errorMessage = response.errorBody()?.string() ?: "로그인에 실패했습니다."
                throw Exception(errorMessage)
            }
        }
    }

    /**
     * 5. 인증 토큰 저장
     * 로컬 저장소에 액세스 토큰과 리프레시 토큰 저장
     *
     * @param tokens 저장할 인증 토큰
     */
    override suspend fun saveTokens(tokens: TokenResponse) {
        userLocalDataSource.saveTokens(tokens)
    }

    /**
     * 6. 인증 토큰 만료 시, 재 요청
     * 401 에러 및 TOKEN_EXPIRED 코드 처리
     * 리프레시 토큰을 사용하여 새 액세스 토큰을 요청
     *
     * @param refreshToken 리프레시 토큰
     * @return 새로운 액세스 토큰, 실패 시 에러와 함께 실패 결과 반환
     */
    override suspend fun refreshAccessToken(refreshToken: String): Result<TokenResponse> {
        return apiCallAfterNetworkCheck {
            val response = apiService.refreshToken(TokenRequest(refreshToken))

            if (response.isSuccessful) {
                val newTokens = response.body() ?: throw Exception("토큰 재발급 응답이 비어 있습니다.")
                userLocalDataSource.saveTokens(newTokens)
                newTokens
            } else {
                val errorMessage = response.errorBody()?.string() ?: "토큰 재발급에 실패했습니다."
                throw Exception(errorMessage)
            }
        }
    }

    /**
     * 7. 기본 도착지(집) 설정
     * 카카오 지도를 통해 검색한 도착지를 서버에 등록
     *
     * @param homeAddress 새로운 도착지 주소
     * @param lat 위도
     * @param lng 경도
     * @return 성공 시 Unit, 실패 시 에러와 함께 실패 결과 반환
     */
    override suspend fun setEndPos(homeAddress: String, lat: Double, lng: Double): Result<Unit> {
        return apiCallAfterNetworkCheck { apiService.setEndPos(homeAddress, lat, lng) }
    }

    /**
     * 8. 보호자 추가
     * 보호자를 서버에 등록 (최대 3명)
     *
     * @param newGuardian 등록할 보호자 정보 (유저와의 관계, 핸드폰 번호)
     * @return 성공 시 Unit, 실패 시 에러와 함께 실패 결과 반환
     */
    override suspend fun addGuardian(newGuardian: Guardian): Result<Unit> {
        return apiCallAfterNetworkCheck { apiService.addGuardian(newGuardian) }
    }
}