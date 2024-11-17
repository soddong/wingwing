package com.ssafy.shieldroneapp.data.repository

import com.ssafy.shieldroneapp.data.model.DroneState
import com.ssafy.shieldroneapp.data.model.request.DroneCancelRequest
import com.ssafy.shieldroneapp.data.model.request.DroneMatchRequest
import com.ssafy.shieldroneapp.data.model.request.DroneRouteRequest
import com.ssafy.shieldroneapp.data.model.response.DroneMatchResponse
import com.ssafy.shieldroneapp.data.model.response.DroneRouteResponse

/**
 * 드론 배정 및 매칭 관련 데이터를 관리하는 리포지토리 인터페이스.
 *
 * 서버와의 API 통신을 통해 드론의 배정 요청, 매칭 상태 확인,
 * 코드 인증 처리 등을 수행하고, 관련된 드론 데이터를 관리합니다.
 * 로컬 데이터 소스(`droneLocalDataSource`)를 통해 배정된 드론 ID를 임시로 저장하고,
 * 코드 인식 성공 여부에 따라 데이터를 적절히 처리합니다.
 */
interface DroneRepository {

    // 드론 배정/매칭 관련
    suspend fun requestDrone(request: DroneRouteRequest): Result<DroneRouteResponse>
    suspend fun cancelDrone(droneId: DroneCancelRequest): Result<Unit>
    suspend fun matchDrone(request: DroneMatchRequest): Result<DroneMatchResponse>

    // 드론 상태 관리
    suspend fun getDroneState(): DroneState?
    suspend fun updateDroneState(state: DroneState)
    suspend fun clearDroneState()

    // 타이머 관련
    suspend fun startAssignmentTimer()
    suspend fun checkAssignmentExpiration(): Boolean
}