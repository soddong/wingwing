package com.ssafy.shieldroneapp.services.sensor

/**
 * 워치에서 수집한 모든 센서 데이터를 로컬에 저장하고, 서버와 동기화하는 서비스 클래스.
 *
 * 워치에서 실시간으로 수집되는 심박수, 가속도계 및 음성 데이터를 로컬에 임시 저장한다.
 * 네트워크 상태에 따라 데이터를 주기적으로 서버에 전송하여 실시간 데이터를 동기화하고,
 * 중단 없는 데이터 흐름을 보장한다.
 *
 * @property sensorDataRepository 센서 데이터를 로컬과 서버에 동기화하는 SensorDataRepository 객체
 * @property webSocketService 서버와의 실시간 통신을 위한 WebSocket 서비스 객체
 */