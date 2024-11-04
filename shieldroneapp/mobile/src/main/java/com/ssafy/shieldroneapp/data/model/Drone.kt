package com.ssafy.shieldroneapp.data.model

/**
 * 드론의 배정 정보 및 매칭 상태를 나타내는 데이터 클래스.
 *
 * @property droneId 드론 ID
 * @property stationIP 드론 스테이션 서버 IP
 * @property startLocation 출발 위치 정보 (Location 객체 사용)
 * @property endLocation 도착 위치 정보 (Location 객체 사용)
 * @property estimatedTime 예상 운행 시간 (분 단위)
 * @property distance 출발지와 도착지 사이의 거리 (km 단위)
 */