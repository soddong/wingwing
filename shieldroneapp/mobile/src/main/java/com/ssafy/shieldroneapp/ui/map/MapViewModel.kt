package com.ssafy.shieldroneapp.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.shieldroneapp.data.model.LatLng
import com.ssafy.shieldroneapp.data.repository.DroneRepository
import com.ssafy.shieldroneapp.data.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Map 화면의 상태와 로직을 관리하는 ViewModel 클래스.
 *
 * 유저의 GPS 위치, 근처 정류장 리스트, 출발지/도착지 정보 등을 관리.
 * 드론 배정 요청, 드론 코드 인식, 위험 상황 알림 등 주요 이벤트를 처리한다.
 * 위치 정보와 드론 관련 데이터를 처리하기 위해 리포지토리와 상호작용한다.
 *
 * @property mapRepository 출발지/도착지 위치 데이터를 관리하는 리포지토리 객체
 * @property droneRepository 드론 배정 및 매칭 관련 데이터를 관리하는 리포지토리 객체
 * @property alertRepository 위험 상황 알림 데이터를 관리하는 리포지토리 객체
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val droneRepository: DroneRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    fun handleEvent(event: MapEvent) {
        when (event) {
            is MapEvent.LoadCurrentLocationAndFetchHives  -> fetchCurrentLocationAndNearbyHives()
            is MapEvent.RequestDroneAssignment -> TODO()
            is MapEvent.StartLocationSelected -> TODO()
            is MapEvent.EndLocationSelected -> TODO()
            is MapEvent.SearchHivesByKeyword -> searchHivesByKeyword(event.keyword)
            is MapEvent.SearchDestination -> TODO()
            is MapEvent.BackPressed -> TODO()
            is MapEvent.StartLocationTracking -> startTrackingLocation()
            is MapEvent.StopLocationTracking -> stopTrackingLocation()
        }
    }

    // 실시간 위치 추적을 시작하는 함수
    private fun startTrackingLocation() {
        _state.update { it.copy(isTrackingLocation = true) }
        viewModelScope.launch {
            mapRepository.getLocationUpdates().collect { newLocation ->
                _state.update { it.copy(currentLocation = newLocation) }
            }
        }
    }

    // TODO: 위치 추적을 중지하는 코드 (필요 시)
    private fun stopTrackingLocation() {
        _state.update { it.copy(isTrackingLocation = false) }
    }

    // 1. 현재 위치(GPS)를 불러오고, 그 위치를 기반으로 근처 출발지(드론 정류장) 조회
    private fun fetchCurrentLocationAndNearbyHives() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val location = mapRepository.getCurrentLocation() // 현재 위치 (LatLng)
                _state.update { it.copy(currentLocation = location, isLoading = false) }

                fetchNearbyHives(location) // LatLng 값을 통해 주변 출발지 (드론 정류장) 조회
            } catch (e: Exception) {
                setError("현재 위치를 불러오는 중 오류가 발생했습니다.")
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    // 2. 최초 주변 출발지 (드론 정류장) 조회
    private fun fetchNearbyHives(location: LatLng) {
        viewModelScope.launch {
            mapRepository.getNearbyHives(location.lat, location.lng)
                .onSuccess { hives ->
                    // HiveResponse 목록을 RouteLocation 목록으로 변환
                    val routeLocations = hives.map { it.toRouteLocation() }
                    _state.update { it.copy(nearbyHives = routeLocations, error = null) }

                    // 성공적으로 받아온 출발지 리스트를 로그로 확인
                    Log.d("MapViewModel", "Nearby Hives: $routeLocations")
                }
                .onFailure { error ->
                    setError("주변 출발지를 불러오는 중 오류가 발생했습니다: ${error.message}")
                }
        }
    }

    // 3. 키워드로 정류장 검색
    private fun searchHivesByKeyword(keyword: String) {
        viewModelScope.launch {
            mapRepository.searchHivesByKeyword(keyword)
                .onSuccess { hives ->
                    // HiveResponse 목록을 RouteLocation 목록으로 변환
                    val routeLocations = hives.map { it.toRouteLocation() }
                    _state.update { it.copy(searchResults = routeLocations, error = null) }
                }
                .onFailure { error ->
                    setError("출발지(드론 정류장) 검색 중 오류가 발생했습니다: ${error.message}")
                }
        }
    }

    // TODO: (확인 필요) 출발지 설정 관련 업데이트
    fun updateStartLocationText(text: String) {
        _state.update { it.copy(startSearchText = text) }
        if (text.length >= 2) searchHivesByKeyword(text)
    }

    // TODO: (확인 필요) 도착지 설정 관련 업데이트
    fun updateEndLocationText(text: String) {
        _state.update { it.copy(endSearchText = text) }
    }

    // 오류 메시지 설정
    private fun setError(message: String) {
        _state.update { it.copy(error = message) }
    }

}