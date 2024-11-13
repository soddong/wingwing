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

fun convertToKakaoLatLng(location: CustomLatLng): LatLng {
    return LatLng.from(location.lat, location.lng)
}

fun setupMap(map: KakaoMap, mapViewModel: MapViewModel) {
    // 1. 기본 줌 레벨 설정
    map.moveCamera(CameraUpdateFactory.zoomTo(15))

    // 2. 현재 위치 로드 및 주변 정류장 조회 요청
    mapViewModel.handleEvent(MapEvent.LoadCurrentLocationAndFetchHives)

    // 3. 마커 클릭 이벤트 설정
    map.setOnLabelClickListener { kakaoMap, labelLayer, label ->
        when (label.tag) {
            is RouteLocation -> {
                val hive = label.tag as RouteLocation
                mapViewModel.handleEvent(MapEvent.StartLocationSelected(hive))
                true
            }
            else -> false
        }
    }
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
    // 모든 레이블 제거
    map.labelManager?.removeAllLabelLayer()
    Log.d("MapUtils", "모든 레이블 제거됨")

    // 새로운 레이어 생성
    val layerOptions = LabelLayerOptions.from(LABEL_LAYER)
    val layer = map.labelManager?.addLayer(layerOptions)

    // 1. 현재 위치 마커 추가
    state.currentLocation?.let { location ->
        val position = convertToKakaoLatLng(location)
        val currentLocationLabel = LabelOptions.from(position)
            .setStyles(LabelStyle.from(R.drawable.ic_current_location))
            .setTag("current_location")

        layer?.addLabel(currentLocationLabel)
        Log.d("MapUtils", "현재 위치 마커 추가됨: $position")

        // 현재 위치로 카메라 이동 (항상 이동하도록 수정)
        map.moveCamera(CameraUpdateFactory.newCenterPosition(position))
    }

    // 2. 드론 정류장 마커 추가
    state.nearbyHives.forEach { hive ->
        val position = LatLng.from(hive.lat, hive.lng)
        val hiveLabel = LabelOptions.from(position)
            .setStyles(LabelStyle.from(R.drawable.ic_location_pin))
            .setClickable(true)
            .setTag(hive)

        layer?.addLabel(hiveLabel)
        Log.d("MapUtils", "드론 정류장 마커 추가: ${hive.hiveName} (위치: $position, 거리: ${hive.distance}m)")
    }
}