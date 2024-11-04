package com.ssafy.shieldroneapp.data.repository

/**
 * 드론의 배정 및 매칭 관련 데이터를 관리하는 리포지토리 클래스.
 *
 * 서버와의 API 통신을 통해 드론의 배정 요청, 매칭 상태 확인,
 * QR 인증 처리 등을 수행하고, 관련된 드론 데이터를 관리한다.
 * 로컬 데이터 소스(`droneLocalDataSource`)를 통해 배정된 드론 ID를 임시로 저장하고,
 * QR 인식 성공 여부에 따라 데이터를 적절히 처리한다.
 *
 * @property apiService 서버와의 통신을 위한 API 서비스 객체
 * @property droneLocalDataSource 로컬 데이터 소스 객체, 드론 배정 상태를 임시로 저장하고 관리한다.
 */