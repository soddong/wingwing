package com.ssafy.shieldroneapp.utils

import android.util.Log
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.ssafy.shieldroneapp.data.model.LatLng as CustomLatLng
import com.ssafy.shieldroneapp.ui.map.MapState
import com.ssafy.shieldroneapp.R
import com.ssafy.shieldroneapp.data.model.RouteLocation
import com.ssafy.shieldroneapp.ui.map.MapEvent
import com.ssafy.shieldroneapp.ui.map.MapViewModel

private const val TAG = "MapUtils - 지도 설정 / 마커"

// 이전 상태를 저장할 변수
private var lastState: MapState? = null

// 상태가 변경되었는지 확인하는 함수
private fun needsUpdate(newState: MapState): Boolean {
    return when {
        lastState == null -> true
        lastState?.currentLocation != newState.currentLocation -> true
        lastState?.selectedStart != newState.selectedStart -> true
        lastState?.selectedEnd != newState.selectedEnd -> true
        lastState?.nearbyHives != newState.nearbyHives -> true
        else -> false
    }
}

fun convertToKakaoLatLng(location: CustomLatLng): LatLng {
    return LatLng.from(location.lat, location.lng)
}

fun setupMap(map: KakaoMap, mapViewModel: MapViewModel) {
    Log.d("MapUtils", "지도 초기 설정 시작")

    // 1. 기본 줌 레벨 설정
    map.moveCamera(CameraUpdateFactory.zoomTo(15))

    // 2. 현재 위치가 있다면 해당 위치로 이동
    mapViewModel.state.value.currentLocation?.let { location ->
        val position = convertToKakaoLatLng(location)
        map.moveCamera(CameraUpdateFactory.newCenterPosition(position))
        Log.d("MapUtils", "초기 카메라 위치 설정: $position")
    }

    // 3. 마커 클릭 이벤트 설정
    map.setOnLabelClickListener { _, _, label ->
        when (label.tag) {
            is RouteLocation -> {
                val hive = label.tag as RouteLocation
                mapViewModel.handleEvent(MapEvent.StartLocationSelected(hive))
                true
            }
            else -> false
        }
    }

    // 4. 상태 업데이트 요청
    mapViewModel.handleEvent(MapEvent.LoadCurrentLocationAndFetchHives)
    Log.d("MapUtils", "지도 초기 설정 완료")
}

// 현재 위치 마커만 업데이트 (실시간 위치 추적용)
fun updateCurrentLocationMarker(map: KakaoMap, location: CustomLatLng?) {
    location?.let {
        // state를 새로 만들어서 현재 위치만 업데이트
        val state = MapState(
            currentLocation = location,
            isTrackingLocation = true
        )
        updateAllMarkers(map, state)
    } ?: Log.d("MapUtils", "Location is null, marker not added")
}

fun updateAllMarkers(map: KakaoMap, state: MapState) {
    Log.d(TAG, "마커 업데이트 시작")

    // 상태가 변경된 경우에만 마커 업데이트 수행
    if (needsUpdate(state)) {
        map.labelManager?.removeAllLabelLayer()

        // 레이어 순서대로 생성 (아래부터 쌓임)
        // 1. 현재 위치 레이어 (맨 아래)
        map.labelManager?.addLayer(LabelLayerOptions.from(Constants.Marker.BASE_LAYER))?.let { layer ->
            state.currentLocation?.let { location ->
                val position = convertToKakaoLatLng(location)
                val currentLocationLabel = LabelOptions.from(position)
                    .setStyles(LabelStyle.from(R.drawable.ic_current_location))
                    .setTag("current_location")
                    .setClickable(false)
                layer.addLabel(currentLocationLabel)

                // 카메라 이동이 필요한 경우만 이동
                if (!map.cameraPosition?.position?.equals(position)!!) {
                    map.moveCamera(CameraUpdateFactory.newCenterPosition(position))
                }
            }
        }

        // 2. 일반 정류장 레이어 (중간)
        map.labelManager?.addLayer(LabelLayerOptions.from(Constants.Marker.HIVE_LAYER))?.let { layer ->
            state.nearbyHives
                .filter { hive -> state.selectedStart?.hiveId != hive.hiveId }  // 선택된 출발지 제외
                .forEach { hive ->
                    val position = LatLng.from(hive.lat, hive.lng)
                    val hiveLabel = LabelOptions.from(position)
                        .setStyles(LabelStyle.from(R.drawable.ic_location_pin))
                        .setTag(hive)
                        .setClickable(true)
                    layer.addLabel(hiveLabel)
                }
        }

        // 3. 선택된 마커 레이어 (맨 위)
        map.labelManager?.addLayer(LabelLayerOptions.from(Constants.Marker.SELECTED_LAYER))?.let { layer ->
            // 선택된 출발지
            state.selectedStart?.let { start ->
                val position = LatLng.from(start.lat, start.lng)
                val startLabel = LabelOptions.from(position)
                    .setStyles(LabelStyle.from(R.drawable.ic_start_location_pin))
                    .setTag(start)
                    .setClickable(false)
                layer.addLabel(startLabel)
            }

            // 선택된 도착지
            state.selectedEnd?.let { end ->
                val position = LatLng.from(end.lat, end.lng)
                val endLabel = LabelOptions.from(position)
                    .setStyles(LabelStyle.from(R.drawable.ic_end_location_pin))
                    .setTag(end)
                    .setClickable(false)
                layer.addLabel(endLabel)
            }
        }

        // 마지막 상태 저장
        lastState = state.copy()
    }

    Log.d("MapUtils", "마커 업데이트 완료")
}
