package com.ssafy.shieldroneapp.services.connection

/**
 * 모바일과 워치 간의 데이터 통신을 관리하는 서비스 클래스.
 *
 * 워치에서 수신한 심박수, 가속도계, 음성 데이터를 모바일로 전달하고, 이를 서버에 전송한다.
 * 모바일-워치 간의 연결 상태를 관리하며, 데이터 수신 및 전송 로직을 포함하여
 * 실시간 데이터 동기화를 위해 연결 상태를 지속적으로 모니터링하고 필요 시 재연결을 시도한다.
 *
 * @property sensorDataRepository 센서 데이터를 로컬과 서버에 동기화하는 SensorDataRepository 객체
 * @property connectionManager 모바일-워치 간 연결을 관리하는 ConnectionManager 객체
 */