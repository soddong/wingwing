package com.ssafy.shieldroneapp.data.source.local

/**
 * 워치에서 수신한 센서 데이터를 로컬에서 관리하는 클래스.
 *
 * 수집한 센서 데이터를 로컬 데이터 소스에 임시로 저장하고, 서버로 전송하기 전까지 데이터를 보관한다.
 * 네트워크 상태에 따라 데이터의 저장과 전송을 관리하며, 간단한 로컬 저장소 방식을 사용한다.
 *
 * @property dataSource 로컬 데이터 소스 객체
 */