package com.ssafy.shieldroneapp.data.repository

/**
 * 사용자 정보를 관리하는 리포지토리 클래스.
 *
 * 서버와의 API 통신을 통해 사용자 인증, 회원가입, 로그인 등의 기능을 수행하고,
 * 로컬 데이터 소스(`userLocalDataSource`)를 통해 사용자 기본 정보 및 보호자 연락처를 관리한다.
 *
 * 인증이 완료되면 ViewModel에서 수집한 사용자 데이터를 로컬에 저장하거나,
 * 필요 시 서버로 전송하여 사용자 정보를 안전하게 관리한다.
 *
 * @property apiService 서버와의 통신을 위한 API 서비스 객체
 * @property userLocalDataSource 로컬 데이터 소스 객체, SharedPreferences를 통해 사용자 데이터를 저장 및 관리한다.
 */