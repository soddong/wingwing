package com.ssafy.shieldroneapp.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.LatLng
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation
import com.ssafy.shieldroneapp.data.model.request.EmergencyRequest
import com.ssafy.shieldroneapp.data.model.request.HiveSearchRequest
import com.ssafy.shieldroneapp.data.model.request.KakaoSearchRequest
import com.ssafy.shieldroneapp.data.repository.AlertRepository
import com.ssafy.shieldroneapp.data.repository.DroneRepository
import com.ssafy.shieldroneapp.data.repository.MapRepository
import com.ssafy.shieldroneapp.data.source.remote.ApiService
import com.ssafy.shieldroneapp.data.source.remote.WebSocketMessageSender
import com.ssafy.shieldroneapp.ui.map.screens.AlertHandler
import com.ssafy.shieldroneapp.ui.components.AlertState
import com.ssafy.shieldroneapp.ui.components.AlertType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val webSocketMessageSender: WebSocketMessageSender,
) : ViewModel() {
    companion object {
        private const val TAG = "MapViewModel 모바일: 맵 뷰모델"
    }

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    // 위치 서비스 활성화 상태를 추적하기 위한 상태 추가
    private val _locationServicesEnabled = MutableStateFlow(false)
    val locationServicesEnabled: StateFlow<Boolean> = _locationServicesEnabled.asStateFlow()

    private val _isWatchConfirmed = MutableStateFlow(false)
    val isWatchConfirmed: StateFlow<Boolean> = _isWatchConfirmed.asStateFlow()

    private val _showToast = MutableStateFlow(false)
    val showToast: StateFlow<Boolean> = _showToast.asStateFlow()

    private var searchJob: Job? = null

    fun handleEvent(event: MapEvent) {
        when (event) {
            is MapEvent.LoadCurrentLocationAndFetchHives -> fetchCurrentLocationAndNearbyHives() // 근처 출발지 조회
            is MapEvent.SearchHivesByKeyword -> searchHivesByKeyword(event.keyword) // 출발지 검색
            is MapEvent.SearchDestination ->  searchDestinationByKeyword(event.destination) // 도착지 검색

            is MapEvent.StartLocationSelected -> selectStartLocation(event.location) // 출발지 마커 선택
            is MapEvent.EndLocationSelected -> TODO() // 도착지 마커 선택
            is MapEvent.CloseModal -> dismissModal() // 모달 닫기

            is MapEvent.SearchFieldClicked -> clickSearchField(event.type) // 검색 입력 필드 클릭 (출발/도착 공통)
            is MapEvent.UpdateStartLocationText -> searchStartLocation(event.text) // 출발지 검색 - 텍스트 입력 시 처리
            is MapEvent.UpdateEndLocationText -> searchEndLocation(event.text) // 도착지 검색 - 텍스트 입력 시 처리

            is MapEvent.SetStartLocation -> setStartLocation(event.location) // 출발지 선택
            is MapEvent.SetEndLocation  -> setEndLocation(event.location) // 도착지 선택

            is MapEvent.RequestDroneAssignment -> TODO() // 드론 배정 요청

            is MapEvent.UpdateLocationServicesState -> updateLocationServicesState(event.isEnabled) // 위치 서비스 활성화 상태 업데이트

            is MapEvent.StartLocationTracking -> startTrackingLocation() // 실시간 위치 추적 시작
            is MapEvent.StopLocationTracking -> stopTrackingLocation() // 실시간 위치 추적 중지
            
            is MapEvent.SetErrorMessage -> setError(event.message) // 오류 메세지 설정
        }
    }

    /**
     * 1-1. 현재 위치(GPS)를 불러오고, 그 위치를 기반으로 근처 출발지(드론 정류장) 조회
     * */
    private fun fetchCurrentLocationAndNearbyHives() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val location = mapRepository.getCurrentLocation() // 현재 위치 (LatLng)
                _state.update { it.copy(currentLocation = location) }
                Log.d(TAG, "현재 위치 확인: $location")

                fetchNearbyHives(location) // LatLng 값을 통해 주변 출발지 (드론 정류장) 조회
            } catch (e: Exception) {
                setError("MapViewModel: 현재 위치를 불러오는 중 오류가 발생했습니다.")
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 1-2. 최초 주변 출발지 (드론 정류장) 조회 (1-1에서 호출)
     * */
    private fun fetchNearbyHives(location: LatLng) {
        viewModelScope.launch {
            Log.d(TAG, "위도, 경도 값: ${location.lat}, ${location.lng}")
            mapRepository.getNearbyHives(location)
                .onSuccess { hives ->
                    // HiveResponse 목록을 RouteLocation 목록으로 변환
                    val routeLocations = hives.map { it.toRouteLocation() }
                    _state.update { it.copy(nearbyHives = routeLocations, error = null) }

                    Log.d(TAG, "초기 근처 정류장 목록: $routeLocations")
                }
                .onFailure { error ->
                    setError("주변 출발지를 불러오는 중 오류가 발생했습니다: ${error.message}")
                    Log.d(TAG, "초기 근처 정류장 목록 불러오기 오류: $error.message")

                }
        }
    }

    /**
     * 2. 키워드로 출발지(드론 정류장) 검색
     * */
    private fun searchHivesByKeyword(keyword: HiveSearchRequest) {
        viewModelScope.launch {
            mapRepository.searchHivesByKeyword(keyword)
                .onSuccess { hives ->
                    // HiveResponse 목록을 RouteLocation 목록으로 변환
                    val routeLocations = hives.map { it.toRouteLocation() }
                    _state.update { it.copy(
                        searchResults = routeLocations,
                        showSearchModal = true, // 검색 결과 모달 표시
                        error = null
                    ) }
                    Log.d(TAG, "출발지 검색 결과, 정류장 리스트: $routeLocations")
                }
                .onFailure { error ->
                    setError("출발지(드론 정류장) 검색 중 오류가 발생했습니다: ${error.message}")
                }
        }
    }

    /**
     * 3. 키워드로 도착지(집) 검색
     * */
    private fun searchDestinationByKeyword(keyword: KakaoSearchRequest) {
        Log.d(TAG, "일단 search 메서드 호출함: $keyword")

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            mapRepository.searchDestinationByKeyword(keyword)
                .onSuccess { searchResults ->
                    Log.d(TAG, "도착지 검색 성공, 결과: $searchResults")
                    _state.update {
                        it.copy(
                            searchResults = searchResults,
                            showSearchModal = true, // 검색 결과 모달 표시
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    Log.d(TAG, "도착지 검색 중 오류 발생: $error.message")
                    setError("Map 도착지 검색 중 오류 발생: ${error.message}")
                    _state.update { it.copy(isLoading = false) }
                }
        }
    }
    
    /**
     * 4. 출발지 마커 - 선택
     * */
    private fun selectStartLocation(location: RouteLocation) {
        _state.update {
            it.copy(
                selectedStartMarker = location,
                showStartMarkerModal = true
            )
        }
        Log.d(TAG,"출발지 마커 모달 표시됨: ${location}")
    }

    /**
     * 5. TODO: 도착지 마커 - 선택
     * */
    private fun selectEndLocation() {}

    /**
     * 6. 모달 닫기
     * */
    private fun dismissModal() {
        _state.update { it.copy(
            showStartMarkerModal = false,
            showSearchModal = false,
        ) }
    }

    /**
     * 7. 출발지/도착지 검색 입력 필드 클릭
     * */
    private fun clickSearchField(type: LocationType) {
        Log.d(TAG, "searchType 아 진짜 왜저래: $type")
        _state.update { it.copy(
            searchType = type,
            showStartMarkerModal = false,
        ) }
    }

    /**
     * 8. 출발지 검색 - 텍스트 입력 시 처리
     * */
    private fun searchStartLocation(text: String) {
        Log.d(TAG, "출발지 검색 입력 값: $text")
        _state.update { it.copy(startSearchText = text) }
        if (text.trim() == "") {
            dismissModal()
        } else {
            searchHivesByKeyword(HiveSearchRequest(text))
        }
    }

    /**
     * 9. 도착지 검색 - 텍스트 입력 시 처리
     * */
    private fun searchEndLocation(text: String) {
        Log.d(TAG, "도착지 검색 입력 값: $text")
        _state.update { it.copy(endSearchText = text) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // 300ms 디바운스
            val trimmedText = text.trim()
            if (trimmedText.isEmpty()) {
                dismissModal()
                return@launch
            }
            searchDestinationByKeyword(KakaoSearchRequest(trimmedText))
        }
    }

    /**
     * 10. 출발지 선택
     * */
    private fun setStartLocation(location: RouteLocation) {
        viewModelScope.launch {
            try {
                _state.update { currentState ->
                    currentState.copy(
                        selectedStart = location,
                        startSearchText = location.locationName ?: "",
                        showStartMarkerModal = true,
                        showSearchModal = false,
                    )
                }
//                mapRepository.saveStartLocation(location)
                Log.d(TAG, "출발지 설정 완료: ${location.locationName}")
            } catch (e: Exception) {
                Log.e(TAG, "출발지 설정 중 오류 발생", e)
                setError("출발지 설정 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 11. 도착지 선택
     * */
    private fun setEndLocation(location: RouteLocation) {
        viewModelScope.launch {
            try {
                _state.update { currentState ->
                    currentState.copy(
                        selectedEnd = location,
                        endSearchText = location.locationName ?: "",
                        showEndMarkerModal = true,
                        showSearchModal = false,
                    )
                }
//                mapRepository.saveEndLocation(location)
                Log.d(TAG, "도착지 설정 완료: ${location.locationName}")
            } catch (e: Exception) {
                Log.e(TAG, "도착지 설정 중 오류 발생", e)
                setError("도착지 설정 중 오류가 발생했습니다.")
            }
        }
    }
    
    /**
     * 12. 드론 배정 요청
     * */

    /**
     * 13. 위치 서비스 활성화 상태 업데이트
     * */
    fun updateLocationServicesState(isEnabled: Boolean) {
        _locationServicesEnabled.value = isEnabled
    }

    /**
     * 14. 실시간 위치 추적을 시작
     * */
    private fun startTrackingLocation() {
        _state.update { it.copy(isTrackingLocation = true) }
        viewModelScope.launch {
            mapRepository.getLocationUpdates().collect { newLocation ->
                _state.update { it.copy(currentLocation = newLocation) }
                Log.d(TAG, "현재 위치 갱신됨: $newLocation")  // 위치 갱신 확인 로그

                try {
                    webSocketMessageSender.sendLocationUpdate(newLocation)
                } catch (e: Exception) {
                    Log.e(TAG, "위치 전송 실패", e)
                    // 실패해도 추적 계속
                }
            }
        }
    }

    /**
     * 15. TODO: 실시간 위치 추적을 중지 (필요 시)
     * */
    private fun stopTrackingLocation() {
        _state.update { it.copy(isTrackingLocation = false) }
    }

    /**
     * 16. 오류 메시지 설정
     * */
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
            val currentLocation = state.value.currentLocation
            if (currentLocation == null) {
                Log.e(TAG, "현재 위치 정보를 가져올 수 없습니다.")
                return false
            }

            val request = EmergencyRequest(
                lat = BigDecimal.valueOf(currentLocation.lat),
                lng = BigDecimal.valueOf(currentLocation.lng)
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