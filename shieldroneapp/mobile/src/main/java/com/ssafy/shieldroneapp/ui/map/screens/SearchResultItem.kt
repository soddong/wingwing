package com.ssafy.shieldroneapp.ui.map.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
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
        Text(
            text = location.locationName ?: "이름 없음",
            style = MaterialTheme.typography.subtitle2,
        )

        Spacer(modifier = Modifier.padding(vertical = 4.dp))

        if (location.locationType == LocationType.START) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${location.direction ?: "N/A"} 방면 | 정류장 번호: ${location.hiveNo ?: "N/A"}",
                    style = MaterialTheme.typography.body2,
                )
//                Text(
//                    text = "이용 가능 드론: ${location.availableDrone ?: 3}개",
//                    style = MaterialTheme.typography.body2,
//                )
            }
        } else {
            Text(
                text = "${location.homeAddress}",
                style = MaterialTheme.typography.body2,
            )
        }
    }
}