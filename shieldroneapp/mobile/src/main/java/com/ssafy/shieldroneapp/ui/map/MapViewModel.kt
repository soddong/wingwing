package com.ssafy.shieldroneapp.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.LatLng
import com.ssafy.shieldroneapp.data.model.request.EmergencyRequest
import com.ssafy.shieldroneapp.data.repository.AlertRepository
import com.ssafy.shieldroneapp.data.repository.DroneRepository
import com.ssafy.shieldroneapp.data.repository.MapRepository
import com.ssafy.shieldroneapp.data.source.remote.ApiService
import com.ssafy.shieldroneapp.ui.map.screens.AlertHandler
import com.ssafy.shieldroneapp.ui.components.AlertState
import com.ssafy.shieldroneapp.ui.components.AlertType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val droneRepository: DroneRepository,
    private val alertRepository: AlertRepository,
    private val alertHandler: AlertHandler,
    private val apiService: ApiService,
) : ViewModel() {
    companion object {
        private const val TAG = "모바일: 맵 뷰모델"
    }

    private val _isWatchConfirmed = MutableStateFlow(false)
    val isWatchConfirmed: StateFlow<Boolean> = _isWatchConfirmed.asStateFlow()

    private val _showToast = MutableStateFlow(false)
    val showToast: StateFlow<Boolean> = _showToast.asStateFlow()

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    fun handleEvent(event: MapEvent) {
        when (event) {
            is MapEvent.LoadCurrentLocationAndFetchHives -> fetchCurrentLocationAndNearbyHives()
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

    val alertState = alertRepository.alertState
        .map { alertData ->
            alertData?.let {
                AlertState(
                    isVisible = true,
                    alertType = when {
                        it.warningFlag -> AlertType.WARNING
                        it.objectFlag -> AlertType.OBJECT
                        else -> AlertType.WARNING
                    },
                    timestamp = it.timestamp
                )
            } ?: AlertState()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlertState()
        )

    fun dismissAlert() {
        alertRepository.clearAlert()
        when (alertState.value.alertType) {
            AlertType.WARNING -> alertHandler.dismissAlert()
            AlertType.OBJECT -> alertHandler.dismissObjectAlert()
        }
    }

    fun updateWatchConfirmation(confirmed: Boolean) {
        _isWatchConfirmed.value = confirmed
    }

    suspend fun sendEmergencyAlert(): Boolean {
        // 워치에서 안전 확인된 경우 API 호출하지 않음
        if (alertHandler.isWatchConfirmed()) {
            return false
        }

        return try {
            val request = EmergencyRequest(
                // TODO: 임시로 좌표 고정
                lat = BigDecimal("37.123"),
                lng = BigDecimal("127.456")
            )
            Log.d(TAG, "전송 요청: ${Gson().toJson(request)}")
            val response = apiService.setEmergency(request)
            if (response.isSuccessful) {
                Log.d(TAG, "긴급 알림 전송 성공 - 위치: $request")
                true
            } else {
                Log.e(TAG, "긴급 알림 전송 실패 - 상태 코드: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "긴급 알림 전송 실패", e)
            Log.e(TAG, "에러 메시지: ${e.message}")
            Log.e(TAG, "에러 원인: ${e.cause}")
            e.printStackTrace()
            false
        }
    }

    fun showToast() {
        _showToast.value = true
    }

    fun hideToast() {
        _showToast.value = false
    }

    fun handleSafeConfirmation() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "안전 확인 처리 완료")
            } catch (e: Exception) {
                Log.e(TAG, "안전 확인 처리 실패", e)
            }
        }
    }

}