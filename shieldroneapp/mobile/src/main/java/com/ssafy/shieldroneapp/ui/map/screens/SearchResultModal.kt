package com.ssafy.shieldroneapp.ui.map.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation

/**
 * 출발지와 도착지 검색 결과를 리스트 형태로 표시하는 모달 컴포넌트.
 *
 * 출발지 타입: 필터링된 정류장 리스트를 제공.
 *    - RouteLocation 클래스의 locationName, hiveNo, direction, availableDrone, distance 형식
 *
 * 도착지 타입: 필터링된 위치 리스트를 제공.
 *    - RouteLocation 클래스의 locationName, homeAddress, distance
 *
 * @param searchResults 검색 결과 리스트
 * @param onItemSelected 리스트 항목 클릭 시 호출되는 콜백 함수
 */
@Composable
fun SearchResultsModal(
    searchType: LocationType,
    searchResults: List<RouteLocation>,
    onItemSelected: (RouteLocation) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss), // 모달 외부 클릭 시 닫기
        backgroundColor = Color.White,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = if (searchType == LocationType.START) "출발지 검색 결과" else "도착지 검색 결과")
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (searchResults.isEmpty()) {
                    item {
                        Spacer(modifier = Modifier.padding(top = 48.dp))
                        Text(
                            text = "검색 결과가 없습니다.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                } else {
                    items(searchResults) { location ->
                        SearchResultItem(
                            location = location,
                            onClick = { onItemSelected(location) }
                        )
                    }
                }
            }
        }
    }
}