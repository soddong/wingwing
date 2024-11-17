package com.ssafy.shieldroneapp.data.repository

import android.content.Context
import android.util.Log
import com.ssafy.shieldroneapp.data.model.DroneState
import com.ssafy.shieldroneapp.data.model.request.DroneCancelRequest
import com.ssafy.shieldroneapp.data.model.request.DroneMatchRequest
import com.ssafy.shieldroneapp.data.model.request.DroneRouteRequest
import com.ssafy.shieldroneapp.data.model.response.DroneMatchResponse
import com.ssafy.shieldroneapp.data.model.response.DroneRouteResponse
import com.ssafy.shieldroneapp.data.source.local.DroneLocalDataSource
import com.ssafy.shieldroneapp.data.source.remote.ApiService
import com.ssafy.shieldroneapp.utils.NetworkUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DroneRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val droneLocalDataSource: DroneLocalDataSource,
    private val context: Context, // Context 주입
) : DroneRepository {
    companion object {
        private const val TAG = "Map - DroneRepositoryImpl"
    }

    /**
     * 1. 드론 배정 요청
     *
     * 서버에 출발지와 도착지 데이터를 전송하여 드론 배정 가능 여부를 확인
     */
    override suspend fun requestDrone(request: DroneRouteRequest): Result<DroneRouteResponse> {
        return NetworkUtils.apiCallAfterNetworkCheck(context) {
            val response = apiService.requestDrone(request)
            Log.d(TAG, "드론 배정 API 응답 결과? $response")

            if (response.isSuccessful) {
                Log.d(TAG, "서버에 드론 배정 요청 후 받은 성공 후 응답: $response")
                response.body() ?: throw Exception("경로 응답이 비어 있습니다.")
            } else {
                Log.d(TAG, "서버에 드론 배정 요청 실패")
                throw Exception("드론 경로 요청 실패")
            }
        }
    }

    /**
     * 2. 드론 배정 취소 요청
     *
     * 서버에 드론 ID를 전송하여 배정을 취소
     */
    override suspend fun cancelDrone(droneId: DroneCancelRequest): Result<Unit> {
        return try {
            NetworkUtils.apiCallAfterNetworkCheck(context) {
                val response = apiService.cancelDrone(droneId)
                if (response.isSuccessful) {
                    Log.d(TAG, "드론 배정 취소 요청 성공")
                    Unit // 성공 시 반환
                } else {
                    Log.d(TAG, "드론 배정 취소 요청 실패")
                    val errorMessage = response.errorBody()?.string() ?: "알 수 없는 에러 발생"
                    throw Exception("드론 배정 취소 실패: $errorMessage") // 실패 시 예외 발생
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "드론 배정 취소 요청 중 오류 발생", e)
            Result.failure(e)
        }
    }

    /**
     * 3. 드론 매칭 요청
     *
     * 서버에 드론 ID와 드론 코드 데이터를 전송하여 매칭 성공 여부를 확인
     */
    override suspend fun matchDrone(request: DroneMatchRequest): Result<DroneMatchResponse> {
        return try {
            Log.d(TAG, "드론 API 보내기 전 request 확인: $request")

            NetworkUtils.apiCallAfterNetworkCheck(context) {
                val response = apiService.matchDrone(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "드론 최종 매칭 성공 !!!!!!")
                    response.body() ?: throw Exception("응답이 비어 있습니다.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "드론 최종 매칭 요청 실패 - 상태 코드: ${response.code()}, 오류 메시지: $errorBody")
                    throw Exception("드론 최종 매칭 요청 실패: 상태 코드 ${response.code()}, 오류 메시지: $errorBody")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "드론 매칭 요청 중 예외 발생", e)
            Result.failure(e)
        }
    }

    /**
     * 4. 드론 상태 조회
     *
     * 로컬 저장소에서 드론 상태 정보를 불러옴
     */
    override suspend fun getDroneState(): DroneState? {
        return droneLocalDataSource.getDroneState()
    }

    /**
     * 5. 드론 상태 업데이트
     *
     * 로컬 저장소에 드론 상태 정보를 업데이트
     */
    override suspend fun updateDroneState(state: DroneState) {
        droneLocalDataSource.updateDroneState(state)
    }

    /**
     * 6. 드론 상태 초기화
     *
     * 로컬 저장소에 저장된 드론 상태 정보 삭제
     */
    override suspend fun clearDroneState() {
        droneLocalDataSource.clearDroneState()
    }

    /**
     * 7. 드론 배정 타이머 시작
     *
     * 로컬 저장소에 드론 배정 후, 최종 매칭 타이머 시작 시간 기록
     */
    override suspend fun startAssignmentTimer() {
        droneLocalDataSource.startAssignmentTimer()
    }

    /**
     * 8. 드론 배정 만료 여부 확인
     *
     * 10분 경과 시 만료
     * 현재 시간과 배정 시작 시간 간의 경과 시간으로 만료 여부 확인
     */
    override suspend fun checkAssignmentExpiration(): Boolean {
        return droneLocalDataSource.checkAssignmentExpiration()
    }
}
