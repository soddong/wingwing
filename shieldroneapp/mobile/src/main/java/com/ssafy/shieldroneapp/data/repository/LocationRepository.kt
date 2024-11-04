package com.ssafy.shieldroneapp.data.repository

/**
 * 출발지 및 도착지와 관련된 위치 정보를 관리하는 리포지토리 클래스.
 *
 * 서버와의 API 통신을 통해 출발지와 도착지 정보를 서버로 전송하거나,
 * 서버에서 받은 위치 정보를 관리한다. 로컬 데이터 소스와의 상호작용을 통해
 * 기본 출발지와 도착지 정보를 로컬에 저장하고, 필요시 이를 불러온다.
 *
 * @property apiService 서버와의 통신을 위한 API 서비스 객체
 * @property locationLocalDataSource 로컬 데이터 소스 객체
 */