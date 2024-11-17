package com.ssafy.shieldroneapp.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.DroneState
import com.ssafy.shieldroneapp.data.model.DroneStatus
import com.ssafy.shieldroneapp.data.model.LatLng
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation
import com.ssafy.shieldroneapp.data.model.request.DroneCancelRequest
import com.ssafy.shieldroneapp.data.model.request.DroneMatchRequest
import com.ssafy.shieldroneapp.data.model.request.DroneRouteRequest
import com.ssafy.shieldroneapp.data.model.request.EmergencyRequest
import com.ssafy.shieldroneapp.data.model.request.EndLocation
import com.ssafy.shieldroneapp.data.model.request.HiveSearchRequest
import com.ssafy.shieldroneapp.data.model.request.KakaoSearchRequest
import com.ssafy.shieldroneapp.data.model.response.DroneRouteResponse
import com.ssafy.shieldroneapp.data.repository.AlertRepository
import com.ssafy.shieldroneapp.data.repository.DroneRepository
import com.ssafy.shieldroneapp.data.repository.MapRepository
import com.ssafy.shieldroneapp.data.source.local.DroneLocalDataSource
import com.ssafy.shieldroneapp.data.source.remote.ApiService
import com.ssafy.shieldroneapp.data.source.remote.WebSocketConnectionManager
import com.ssafy.shieldroneapp.data.source.remote.WebSocketMessageSender
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import com.ssafy.shieldroneapp.services.manager.AudioServiceManager
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
    private val droneLocalDataSource: DroneLocalDataSource,
    private val alertRepository: AlertRepository,
    private val alertHandler: AlertHandler,
    private val apiService: ApiService,
    private val webSocketMessageSender: WebSocketMessageSender,
    private val webSocketService: WebSocketService,
    private val webSocketConnectionManager: WebSocketConnectionManager,
    private val audioServiceManager: AudioServiceManager,
) : ViewModel() {
    companion object {
        private const val TAG = "MapViewModel 모바일: 맵 뷰모델"
        private const val TIMER_DURATION_MS = 10 * 60 * 1000L // 10분
        private const val SEARCH_DEBOUNCE_MS = 150L // 150ms 디바운스
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

    private var searchJob: Job? = null // 검색 디바운스 용도
    private var timerJob: Job? = null // 10분 타이머 용도

    // 앱 실행 시 로컬 드론 상태 로드
    init {
        handleEvent(MapEvent.LoadDroneState)
        handleEvent(MapEvent.LoadStartAndEndLocations)
    }

    // 전체 이벤트 핸들러
    fun handleEvent(event: MapEvent) {
        when (event) {
            // 출발지/도착지 리스트 조회/검색
            is MapEvent.LoadCurrentLocationAndFetchHives -> fetchCurrentLocationAndNearbyHives() // 근처 출발지 조회
            is MapEvent.SearchHivesByKeyword -> searchHivesByKeyword(event.keyword) // 출발지 검색
            is MapEvent.SearchDestination -> searchDestinationByKeyword(event.destination) // 도착지 검색

            // 출발지/도착지 마커 선택
            is MapEvent.StartLocationSelected -> selectStartLocation(event.location) // 출발지 마커 선택
            is MapEvent.EndLocationSelected -> selectEndLocation(event.location) // 도착지 마커 선택

            // 모달 관리
            is MapEvent.OpenModal -> openModal(event.modalType)
            is MapEvent.CloseModal -> closeModal(event.modalType)
            is MapEvent.CloseAllModals -> closeAllModals()

            // 출발지/도착지 검색 입력 필드 클릭 / 텍스트 입력 시
            is MapEvent.SearchFieldClicked -> clickSearchField(event.type) // 검색 입력 필드 클릭 (출발/도착 공통)
            is MapEvent.UpdateStartLocationText -> searchStartLocation(event.text) // 출발지 검색 - 텍스트 입력 시 처리
            is MapEvent.UpdateEndLocationText -> searchEndLocation(event.text) // 도착지 검색 - 텍스트 입력 시 처리

            // 출발지/도착지 선택
            is MapEvent.SetStartLocation -> setStartLocation(event.location) // 출발지 선택
            is MapEvent.SetEndLocation -> setEndLocation(event.location) // 도착지 선택

            // 드론 배정 요청 / 배정 취소 / 최종 매칭 요청 / 결과 처리
            is MapEvent.RequestDroneAssignment -> requestDroneAssignment() // 드론 배정 요청
            is MapEvent.RequestDroneCancel -> requestDroneCancel(event.droneId) // 배정 취소
            is MapEvent.RequestDroneMatching -> requestDroneMatching(event.request) // 드론 최종 매칭 요청
            is MapEvent.HandleDroneMatchingResult -> handleMatchingResult(event.result) // 드론 매칭 결과 별 이벤트 처리

            // 위치 서비스(GPS, 네트워크)의 활성화 상태 업데이트
            is MapEvent.UpdateLocationServicesState -> updateLocationServicesState(event.isEnabled) // 위치 서비스 활성화 상태 업데이트

            // 실시간 위치 추적 이벤트
            is MapEvent.StartLocationTracking -> startTrackingLocation() // 실시간 위치 추적 시작
            is MapEvent.StopLocationTracking -> stopTrackingLocation() // 실시간 위치 추적 중지

            // 오류 메시지 설정
            is MapEvent.SetErrorMessage -> setError(event.message) // 오류 메세지 설정

            // [로컬 저장소] 드론 상태 관리
            is MapEvent.LoadDroneState -> loadDroneState() // 드론 상태 가져오기
            is MapEvent.SaveDroneState -> saveDroneState(event.droneState) // 드론 상태 저장
            is MapEvent.ClearDroneState -> clearDroneState() // 드론 상태 초기화

            // [로컬 저장소] 출발지/도착치 관리
            is MapEvent.LoadStartAndEndLocations -> loadStartAndEndLocations() // 출발지, 도착지 가져오기
            is MapEvent.SaveStartLocation -> saveStartLocation(event.location) // 출발지 저장
            is MapEvent.SaveEndLocation -> saveEndLocation(event.location) // 도착지 저장
            is MapEvent.ClearLocationData -> clearLocationData() // 초기화
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
                    _state.update {
                        it.copy(
                            searchResults = routeLocations,
                            showSearchResultsModal = true, // 검색 결과 모달 표시
                            error = null
                        )
                    }
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
                            showSearchResultsModal = true, // 검색 결과 모달 표시
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
                showStartMarkerModal = true,
            )
        }
        Log.d(TAG, "출발지 마커 모달 표시됨: ${location}")
    }

    /**
     * 5. TODO: 도착지 마커 - 선택
     * */
    private fun selectEndLocation(location: RouteLocation) {}

    /**
     * 6. 특정 모달 열기
     */
    private fun openModal(modalType: ModalType) {
        _state.update {
            when (modalType) {
                ModalType.SEARCH_RESULTS -> it.copy(showSearchResultsModal = true)
                ModalType.START_MARKER_INFO -> it.copy(showStartMarkerModal = true)
                ModalType.END_MARKER_INFO -> it.copy(showEndMarkerModal = true)
                ModalType.DRONE_MATCH_RESULT -> it.copy(showDroneMatchResultModal = true)
                else -> it
            }
        }
    }

    /**
     * 7. 특정 모달 닫기
     */
    private fun closeModal(modalType: ModalType) {
        _state.update {
            when (modalType) {
                ModalType.SEARCH_RESULTS -> it.copy(showSearchResultsModal = false)
                ModalType.START_MARKER_INFO -> it.copy(showStartMarkerModal = false)
                ModalType.END_MARKER_INFO -> it.copy(showEndMarkerModal = false)
                ModalType.DRONE_MATCH_RESULT -> it.copy(showDroneMatchResultModal = false)
                else -> it
            }
        }
    }

    /**
     * 8. 전체 모달 닫기
     */
    private fun closeAllModals() {
        _state.update {
            it.copy(
                showSearchResultsModal = false,
                showStartMarkerModal = false,
                showEndMarkerModal = false,
                showDroneMatchResultModal = false,
                showDroneAssignmentSuccessModal = false,
                showDroneAssignmentFailureModal = false,
                showCancelSuccessModal = false,
            )
        }
    }

    /**
     * 9. 출발지/도착지 검색 입력 필드 클릭
     * */
    private fun clickSearchField(type: LocationType) {
        Log.d(TAG, "searchType 아 진짜 왜저래: $type")
        _state.update { it.copy(searchType = type,) }
        closeAllModals()
    }

    /**
     * 10. 출발지 검색 - 텍스트 입력 시 처리
     * */
    private fun searchStartLocation(text: String) {
        _state.update { it.copy(startSearchText = text) }
        viewModelScope.launch {
            if (text.trim() == "") {
                // 배정된 상태가 아닌 경우에만 로컬 데이터를 지움
                val droneState = _state.value.droneState
                if (droneState?.matchStatus != DroneStatus.MATCHING_ASSIGNED) {
                    _state.update { it.copy(selectedStart = null) }
                    mapRepository.clearStartLocation()
                    Log.d(TAG, "출발지 정보 로컬에서 삭제")
                } else {
                    Log.d(TAG, "드론이 배정된 상태이므로 출발지 정보를 유지")
                }
                closeAllModals()
            } else {
                searchHivesByKeyword(HiveSearchRequest(text))
            }
        }
    }

    /**
     * 11. 도착지 검색 - 텍스트 입력 시 처리
     * */
    private fun searchEndLocation(text: String) {
        _state.update { it.copy(endSearchText = text) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS) // 150ms 디바운스
            val trimmedText = text.trim()
            if (trimmedText.isEmpty()) {
                // 배정된 상태가 아닌 경우에만 로컬 데이터를 지움
                val droneState = _state.value.droneState
                if (droneState?.matchStatus != DroneStatus.MATCHING_ASSIGNED) {
                    _state.update { it.copy(selectedEnd = null) }
                    mapRepository.clearEndLocation()
                    Log.d(TAG, "도착지 정보 로컬에서 삭제")
                } else {
                    Log.d(TAG, "드론이 배정된 상태이므로 도착지 정보를 유지")
                }
                closeAllModals()
                return@launch
            } else {
                searchDestinationByKeyword(KakaoSearchRequest(trimmedText))
            }
        }
    }

    /**
     * 12. 출발지 선택
     * */
    private fun setStartLocation(location: RouteLocation) {
        viewModelScope.launch {
            try {
                _state.update { currentState ->
                    currentState.copy(
                        selectedStart = location,
                        startSearchText = location.locationName ?: "",
                        showStartMarkerModal = true,
                        showSearchResultsModal = false,
                    )
                }
                mapRepository.saveStartLocation(location)
                Log.d(TAG, "출발지 설정 완료: ${location}")
            } catch (e: Exception) {
                Log.e(TAG, "출발지 설정 중 오류 발생", e)
                setError("출발지 설정 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 13. 도착지 선택
     * */
    private fun setEndLocation(location: RouteLocation) {
        viewModelScope.launch {
            try {
                _state.update { currentState ->
                    currentState.copy(
                        selectedEnd = location,
                        endSearchText = location.locationName ?: "",
                        showEndMarkerModal = true,
                        showSearchResultsModal = false,
                    )
                }
                mapRepository.saveEndLocation(location)
                webSocketMessageSender.setDestinationLocation(LatLng(location.lat, location.lng))
                Log.d(TAG, "도착지 설정 완료: ${location}")
            } catch (e: Exception) {
                Log.e(TAG, "도착지 설정 중 오류 발생", e)
                setError("도착지 설정 중 오류가 발생했습니다.")
            }
        }
    }
    
    /**
     * 14-1. 드론 배정 요청
     * */
    private fun requestDroneAssignment() {
        val startLocation = _state.value.selectedStart
        val endLocation = _state.value.selectedEnd

        if (startLocation == null || endLocation == null) {
            setError("출발지와 도착지를 모두 설정해야 합니다.")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val request = DroneRouteRequest(
                hiveId = startLocation.hiveId!!,
                endLocation = EndLocation(lat = endLocation.lat, lng = endLocation.lng)
            )

            droneRepository.requestDrone(request)
                .onSuccess { response ->
                    handleDroneAssignmentSuccess(response)
                }.onFailure { error ->
                    val errorMessage = error.message ?: "드론 배정 요청 중 오류 발생"
                    handleDroneAssignmentFailure(errorMessage)
                }

            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 14-2. 드론 배정 요청 성공 시 결과를 처리
     */
    private fun handleDroneAssignmentSuccess(response: DroneRouteResponse) {
        val droneState = DroneState.createDroneStateFromResponse(response)

        _state.update { currentState ->
            currentState.copy(
                droneState = droneState,
                showDroneAssignmentSuccessModal = true, // 성공 모달 표시
                error = null,
            )
        }
        handleEvent(MapEvent.SaveDroneState(droneState)) // 로컬에 드론 상태 저장
        startTimer() // 배정 완료 후, 매칭까지 10분 타이머 시작
        Log.d(TAG, "드론 배정 성공: $response")
    }

    /**
     * 14-3. 드론 배정 요청 실패 시 에러 메시지를 처리
     */
    private fun handleDroneAssignmentFailure(errorMessage: String) {
        _state.update { currentState ->
            currentState.copy(
                droneAssignmentError = errorMessage,
                showDroneAssignmentFailureModal = true, // 실패 모달 표시
            )
        }
        setError(errorMessage)
        Log.e(TAG, "드론 배정 실패: $errorMessage")
    }

    /**
     * 14-4. 드론 배정 후, 타이머 시작 (10분)
     * */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            delay(10 * 60 * 1000L) // 10분 타이머
            handleMatchingResult(DroneStatus.MATCHING_TIMEOUT)
        }
    }

    /**
     * 15. 10분 경과 하면, 드론 배정 취소 요청
     * */
    private fun requestDroneCancel(droneId: DroneCancelRequest) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                droneRepository.cancelDrone(droneId)
                    .onSuccess {
                        Log.d(TAG, "드론 배정 취소 성공: 드론 ID - ${droneId.droneId}")
                        _state.update { it.copy(
                            selectedStart = null,
                            selectedEnd = null,
                            selectedStartMarker = null,
                            selectedEndMarker = null,
                            startSearchText = "",
                            endSearchText = "",
                            searchResults = emptyList(),
                            droneState = null,
                            droneMatchResult = null, // 매칭 결과 초기화
                            droneAssignmentError = null,
                            showCancelSuccessModal = true, // 취소 성공 알림 표시
                            error = null,
                        ) }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "드론 배정 취소 실패: ${error.message}")
                        setError("드론 배정 취소 중 오류 발생: ${error.message}")
                    }

            } catch (e: Exception) {
                Log.e(TAG, "드론 배정 취소 요청 중 오류 발생", e)
                setError("드론 배정 취소 요청 중 오류가 발생했습니다.")
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 16. 드론 최종 매칭 요청
     * */
    private fun requestDroneMatching(request: DroneMatchRequest) {
        viewModelScope.launch {
            droneRepository.matchDrone(request)
                .onSuccess { response ->
                    // 드론 매칭 성공 시에만 WebSocket 초기화
                    audioServiceManager.initializeWebSocket()

                    _state.update {
                        it.copy(
                            droneMatchResult = response,
                            showDroneMatchResultModal = true,
                            error = null,
                            isLoading = false
                        )
                    }
                    Log.d(TAG, "드론 매칭 성공: $response")
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            showDroneMatchResultModal = true, // 매칭 결과 모달 표시
//                            error = "입력된 인증 코드가 일치하지 않습니다.\n드론에 표시된 코드를 확인해주세요.",
                            error = "${error.message}",
                            isLoading = false
                        )
                    }
                    Log.e(TAG, "드론 매칭 실패: ${error.message}")
                }
        }
    }

    /**
     * 17. 드론 매칭 결과 별, 이벤트 처리
     */
    private fun handleMatchingResult(result: DroneStatus) {
        when (result) {
            DroneStatus.MATCHING_ASSIGNED -> {
                _state.update { it.copy(showDroneAssignmentSuccessModal = true) }
                Log.d(TAG, "드론 배정 성공 모달 활성화")
            }

            DroneStatus.MATCHING_FAILED -> {
                _state.update { it.copy(error = "드론 매칭 실패: 코드 오류") }
                Log.e(TAG, "드론 매칭 실패")
            }

            DroneStatus.MATCHING_COMPLETE -> {
                _state.update { it.copy(error = null) }
                Log.d(TAG, "드론 매칭 완료")
            }
            // 필요에 따라 다른 상태 추가 처리
            else -> {
                Log.d(TAG, "매칭 결과 처리: $result")
            }
        }
    }

    // 결과 처리에서 이것까지 해야 함? ======> 성공 알림 및 애니메이션 시작
//    private fun showSuccessAlert() {
//        _state.update { it.copy(isSuccessAlertVisible = true) }
//        // 애니메이션 실행 로직 호출
//    }

    /**
     * 18. 위치 서비스 활성화 상태 업데이트
     * */
    fun updateLocationServicesState(isEnabled: Boolean) {
        _locationServicesEnabled.value = isEnabled
    }

    /**
     * 19. 실시간 위치 추적을 시작
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
//                    Log.e(TAG, "위치 전송 실패", e)
                    // 실패해도 추적 계속
                }
            }
        }
    }

    /**
     * 20. TODO: 실시간 위치 추적을 중지 (필요 시)
     * */
    private fun stopTrackingLocation() {
        _state.update { it.copy(isTrackingLocation = false) }
    }

    /**
     * 21. 오류 메시지 설정
     * */
    private fun setError(message: String) {
        _state.update { it.copy(error = message) }
    }

    /**
     * 22. 로컬에서 드론 상태 가져오기
     * */
    private fun loadDroneState() {
        viewModelScope.launch {
            val savedState = droneLocalDataSource.getDroneState()
            if (savedState != null) {
                _state.update { it.copy(droneState = savedState) }
            }
        }
    }

    /**
     * 23. 로컬에 드론 상태 저장
     * */
    private fun saveDroneState(droneState: DroneState) {
        viewModelScope.launch {
            droneLocalDataSource.saveDroneState(droneState)
            _state.update { it.copy(droneState = droneState) }
            Log.d(TAG, "드론 상태 로컬 저장 완료: $droneState")
        }
    }

    /**
     * 24. 로컬에서 드론 상태 초기화 및 웹소켓 연결 해제
     */
    private fun clearDroneState() {
        viewModelScope.launch {
            droneLocalDataSource.clearDroneState()
            webSocketService.shutdown()
            _state.update {
                it.copy(
                    droneState = DroneState(
                        droneId = -1, // 기본 드론 ID
                        stationIP = null,
                        matchStatus = DroneStatus.MATCHING_NONE, // 상태 초기화
                        battery = null,
                        estimatedTime = null,
                        distance = null,
                        assignedTime = null,
                    ),
                    showCancelSuccessModal = true // 배정 취소 모달 표시
                )
            }
            Log.d(TAG, "드론 상태 초기화 및 matchStatus: ${DroneStatus.MATCHING_NONE}")
        }
    }

    /**
     * 25. 로컬에서 출발지, 도착지 가져오기
     * */
    private fun loadStartAndEndLocations() {
        viewModelScope.launch {
            try {
                val startLocation = mapRepository.getStartLocation()
                val endLocation = mapRepository.getEndLocation()

                _state.update { currentState ->
                    currentState.copy(
                        selectedStart = startLocation,
                        startSearchText = startLocation?.locationName ?: "",
                        selectedEnd = endLocation,
                        endSearchText = endLocation?.locationName ?: ""
                    )
                }

                Log.d(TAG, "로컬 저장소에서 출발지와 도착지 로드 성공")
            } catch (e: Exception) {
                Log.e(TAG, "로컬 저장소에서 위치 로드 실패", e)
            }
        }
    }

    /**
     * 26. 로컬에서 출발지 저장
     * */
    private fun saveStartLocation(location: RouteLocation) {
        viewModelScope.launch {
            try {
                mapRepository.saveStartLocation(location)
                _state.update { currentState ->
                    currentState.copy(
                        selectedStart = location,
                        startSearchText = location.locationName ?: ""
                    )
                }

                Log.d(TAG, "출발지 로컬 저장 성공: $location")
            } catch (e: Exception) {
                Log.e(TAG, "출발지 로컬 저장 실패", e)
            }
        }
    }


    /**
     * 27. 로컬에서 도착지 저장
     * */
    private fun saveEndLocation(location: RouteLocation) {
        viewModelScope.launch {
            try {
                mapRepository.saveEndLocation(location)
                _state.update { currentState ->
                    currentState.copy(
                        selectedEnd = location,
                        endSearchText = location.locationName ?: ""
                    )
                }

                Log.d(TAG, "도착지 로컬 저장 성공: $location")
            } catch (e: Exception) {
                Log.e(TAG, "도착지 로컬 저장 실패", e)
            }
        }
    }


    /**
     * 27. 로컬에서 출발/도착지 초기화
     * */
    private fun clearLocationData() {
        viewModelScope.launch {
            try {
                mapRepository.clearStartLocation()
                mapRepository.clearEndLocation()
                _state.update { currentState ->
                    currentState.copy(
                        selectedStart = null,
                        startSearchText = "",
                        selectedEnd = null,
                        endSearchText = ""
                    )
                }

                Log.d(TAG, "로컬에서 출발지와 도착지 초기화 성공")
            } catch (e: Exception) {
                Log.e(TAG, "로컬에서 위치 초기화 실패", e)
            }

            // 드론 상태 초기화 시 웹소켓도 연결 해제
            webSocketService.shutdown()
        }
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

    /**
     * WebSocket 연결 초기화
     */
    private fun initializeWebSocket() {
        try {
            Log.d(TAG, "WebSocket 연결 초기화 시작")
            webSocketService.setConnectionManager(webSocketConnectionManager)
            webSocketService.initialize()
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket 초기화 실패", e)
            _state.update {
                it.copy(error = "실시간 연결 설정에 실패했습니다: ${e.message}")
            }
        }
    }

    // 앱 종료 시 웹소켓 연결 해제
    override fun onCleared() {
        super.onCleared()
        webSocketService.shutdown()
    }
}