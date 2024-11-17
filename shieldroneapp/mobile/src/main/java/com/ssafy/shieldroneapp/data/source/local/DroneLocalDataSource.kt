package com.ssafy.shieldroneapp.data.source.local

import com.ssafy.shieldroneapp.data.model.DroneState

/**
 * 드론 배정/매칭 및 상태 정보를 로컬에 저장하고 조회하는 기능을 제공합니다.
 *
 * [주요 기능]
 * - 드론 상태 저장/조회/삭제 기능을 통해 드론 배정/매칭 상태를 로컬에서 관리합니다.
 * - 배정 시작 시간을 기준으로 10분이 경과했는지를 확인하여 배정 만료 여부를 반환합니다.
 */
interface DroneLocalDataSource {
    // 드론 상태 관리
    suspend fun saveDroneState(droneState: DroneState) // 저장
    suspend fun getDroneState(): DroneState? // 조회
    suspend fun updateDroneState(newDroneState: DroneState) // 업데이트
    suspend fun clearDroneState() // 삭제

    // 드론 배정 후 매칭 타이머 관련
    suspend fun startAssignmentTimer()
    suspend fun checkAssignmentExpiration(): Boolean
}