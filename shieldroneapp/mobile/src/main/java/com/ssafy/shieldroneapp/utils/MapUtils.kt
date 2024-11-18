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

private const val LABEL_LAYER = "label_layer"
private const val TAG = "MapUtils - 지도 설정 / 마커"

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
    Log.d(TAG, "마커 업데이트 시작 - 현재 위치: ${state.currentLocation}, 정류장 수: ${state.nearbyHives.size}")

    // 모든 레이블 제거
    map.labelManager?.removeAllLabelLayer()

    // 새로운 레이어 생성
    val layerOptions = LabelLayerOptions.from(LABEL_LAYER)
    val layer = map.labelManager?.addLayer(layerOptions) ?: return

    // 1. 현재 위치 마커 추가
    state.currentLocation?.let { location ->
        val position = convertToKakaoLatLng(location)
        val currentLocationLabel = LabelOptions.from(position)
            .setStyles(LabelStyle.from(R.drawable.ic_current_location))
            .setTag("current_location")
            .setClickable(false)  // 현재 위치 마커는 클릭 불가능하게 설정
        layer.addLabel(currentLocationLabel)

        // 화면 회전 시에는 카메라 이동을 하지 않도록 수정
        if (!map.cameraPosition?.position?.equals(position)!!) {
            map.moveCamera(CameraUpdateFactory.newCenterPosition(position))
            Log.d("MapUtils", "카메라 위치 업데이트: $position")
        }
    }

    // 2. 드론 정류장 마커 추가 (반드시 현재 위치 마커 다음에 추가)
    state.nearbyHives.forEach { hive ->
        val position = LatLng.from(hive.lat, hive.lng)
        val hiveLabel = LabelOptions.from(position)
            .setStyles(LabelStyle.from(R.drawable.ic_location_pin))
            .setTag(hive)
            .setClickable(true)
        layer.addLabel(hiveLabel)
    }

    // 3. 선택된 출발지 마커 추가 (파란색 핀)
    state.selectedStart?.let { start ->
        val position = LatLng.from(start.lat, start.lng)
        val startLabel = LabelOptions.from(position)
            .setStyles(LabelStyle.from(R.drawable.ic_start_location_pin))
            .setTag(start)
            .setClickable(false)
        layer.addLabel(startLabel)
    }

    // 4. 선택된 도착지 마커 추가 (빨간색 핀)
    state.selectedEnd?.let { end ->
        val position = LatLng.from(end.lat, end.lng)
        val endLabel = LabelOptions.from(position)
            .setStyles(LabelStyle.from(R.drawable.ic_end_location_pin))
            .setTag(end)
            .setClickable(false)
        layer.addLabel(endLabel)
    }

    Log.d("MapUtils", "마커 업데이트 완료")
}