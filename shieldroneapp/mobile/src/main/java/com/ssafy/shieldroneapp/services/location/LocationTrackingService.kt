package com.ssafy.shieldroneapp.services.location

/**
 * 사용자의 실시간 위치를 추적하고 서버로 전송하는 서비스 클래스.
 *
 * GPS를 통해 사용자의 현재 위치를 실시간으로 추적하며, 경로 안내 화면에서 위치 정보를 지속적으로 갱신한다.
 * WebSocket을 통해 서버와 위치 정보를 실시간으로 동기화하여, 경로 안내에 필요한 최신 위치 데이터를 제공한다.
 *
 * @property locationManager 위치 정보를 수집하기 위한 LocationManager 객체
 * @property webSocketService 위치 정보를 실시간으로 서버에 전송하기 위한 WebSocket 서비스 객체
 * @property currentLocation 현재 GPS 위치를 저장하는 변수 (필요 시 추가)
 */