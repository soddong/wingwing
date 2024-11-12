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

fun convertToKakaoLatLng(location: CustomLatLng): LatLng {
    return LatLng.from(location.lat, location.lng)
}

fun setupMap(map: KakaoMap, state: MapState) {
    // 초기 카메라 위치 설정
    map.moveCamera(CameraUpdateFactory.zoomTo(15))

    state.currentLocation?.let { location ->
        val position = convertToKakaoLatLng(location)
        map.moveCamera(CameraUpdateFactory.newCenterPosition(position)) // 카메라 이동
        updateCurrentLocationMarker(map, location)
    }
}

// 현재 위치 마커 업데이트 함수
fun updateCurrentLocationMarker(map: KakaoMap, location: CustomLatLng?) {
    location?.let {
        // 모든 레이블 레이어 제거
        map.labelManager?.removeAllLodLabelLayer()
        Log.d("MapUtils", "All label layers removed") // 레이블 레이어 제거 로그

        // 새로운 레이블 추가
        val position = convertToKakaoLatLng(it)
        val labelOptions = LabelOptions.from(position)
            .setStyles(LabelStyle.from(R.drawable.ic_current_location))

        val layerOptions = LabelLayerOptions.from("current_location_layer")
        map.labelManager?.addLayer(layerOptions)?.addLabel(labelOptions)

        Log.d("MapUtils", "Current location marker added at position: $position") // 마커 추가 로그

    }  ?: Log.d("MapUtils", "Location is null, marker not added") // 위치가 null인 경우 로그
}