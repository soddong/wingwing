package com.ssafy.shieldroneapp.ui.map

/**
 * 사용자의 현재 위치를 실시간으로 추적하고 지도에 표시하는 클래스.
 *
 * GPS를 통해 실시간으로 현재 위치를 수집하며, 출발지와 도착지를 기준으로
 * 사용자가 경로를 따라 이동할 때 위치를 업데이트한다.
 *
 * 실시간 위치 데이터는 Map에 현재 위치 아이콘으로 표시된다.
 * 경로 안내 중에도 위치를 지속적으로 업데이트하여 정확한 위치 정보를 제공한다.
 *
 * @property locationManager 위치 정보를 수집하기 위한 LocationManager 객체
 */