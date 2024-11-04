package com.ssafy.shieldroneapp.data.repository

/**
 * 센서 데이터를 관리하는 리포지토리 클래스.
 *
 * 로컬 데이터 소스를 통해 수신한 센서 데이터를 임시로 저장하고,
 * 서버와의 WebSocket 통신을 통해 실시간으로 센서 데이터를 전송한다.
 * 네트워크 상태에 따라 데이터의 저장 및 전송을 조정한다.
 *
 * @property webSocketService 서버와의 실시간 통신을 위한 WebSocket 서비스 객체
 * @property sensorLocalDataSource 로컬 데이터 소스 객체
 */