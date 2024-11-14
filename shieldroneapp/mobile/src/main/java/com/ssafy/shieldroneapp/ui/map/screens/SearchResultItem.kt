package com.ssafy.shieldroneapp.ui.map.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation

@Composable
fun SearchResultItem(
    location: RouteLocation,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(text = location.locationName ?: "이름 없음")
        if (location.locationType == LocationType.START) {
            Text(text = "정류장 번호: ${location.hiveNo ?: "N/A"}")
            Text(text = "방면: ${location.direction ?: "N/A"}")
            Text(text = "거리: ${location.distance ?: 0}m")
        } else {
            Text(text = "도로명 주소: ${location.homeAddress ?: "N/A"}")
            Text(text = "거리: ${location.distance ?: 0}m")
        }
    }
}