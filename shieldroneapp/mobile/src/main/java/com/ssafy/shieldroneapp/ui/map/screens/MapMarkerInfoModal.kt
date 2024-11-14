package com.ssafy.shieldroneapp.ui.map.screens

/**
 * MapMarker에 대한 상세 정보를 표시하는 모달 컴포넌트.
 *
 * 출발지 타입
 *   - 근처 드론 정류장(출발지 후보) 중에 선택한 경우: 선택 버튼이 포함된 모달.
 *   - 검색을 통해 선택한 경우: 선택 버튼이 없는 모달.
 *   - 정류장 이름, 정류장 번호, 이동 방면 정보를 UI에 표시한다.
 *
 * 도착지 타입
 *   - 선택 버튼이 없는 모달.
 *   - 정류장 이름, 도로명 주소, 현재 위치로부터의 거리를 UI에 표시한다.
 *
 * @param locationType 위치 유형 (출발지 OR 도착지)
 * @param locationName 선택된 위치의 이름
 *
 * @param hiveNo 정류장 번호 (출발지)
 * @param direction 이동 방면 정보 (출발지)
 * @param onConfirm 선택 버튼 클릭 시 호출되는 콜백 함수 (출발지 후보 선택 시에만 사용)
 *
 * @param homeAddress 도로명 주소 (도착지)
 * @param distance 현재 위치로부터의 거리 (도착지)
 */
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation

@Composable
fun MapMarkerInfoModal(
    routeLocation: RouteLocation,
    onSelect: (() -> Unit)? = null // 선택 버튼이 필요한 경우에만 사용
) {
    val borderColor = if (routeLocation.locationType == LocationType.START) Color.Blue else Color.Red
    val showSelectButton = routeLocation.locationType == LocationType.START

    Card (
        modifier = Modifier
            .widthIn(max = 340.dp)
            .padding(horizontal = 16.dp)
            .wrapContentSize(),
        border = BorderStroke(2.dp, borderColor), // 위치 유형에 따른 border 색상
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TODO: availableDrone 개수, battery 정보 등 추가해야 함
            Text(
                text = routeLocation.locationName ?: "이름 없음",
                style = MaterialTheme.typography.h6
            )
            if (routeLocation.locationType == LocationType.START) {
                Text(text = "정류장 번호: ${routeLocation.hiveNo ?: "N/A"}")
                Text(text = "방면: ${routeLocation.direction ?: "N/A"}")
            } else {
                Text(text = "도로명 주소: ${routeLocation.homeAddress ?: "N/A"}")
                Text(text = "거리: ${routeLocation.distance ?: 0}m")
            }


            if (showSelectButton) {
                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onSelect?.invoke() },
                    modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)

                ) {
                    Text(
                        text = "출발지로 선택",
                        style = MaterialTheme.typography.h6,
                    )
                }
            }
        }
    }
}