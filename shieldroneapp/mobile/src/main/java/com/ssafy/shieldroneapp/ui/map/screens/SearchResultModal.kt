package com.ssafy.shieldroneapp.ui.map.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation
import com.ssafy.shieldroneapp.utils.rememberKeyboardController
import kotlinx.coroutines.launch

/**
 * 출발지와 도착지 검색 결과를 리스트 형태로 표시하는 모달 컴포넌트.
 *
 * 출발지 타입: 필터링된 정류장 리스트를 제공.
 *    - RouteLocation 클래스의 locationName, hiveNo, direction, availableDrone, distance 형식
 *
 * 도착지 타입: 필터링된 위치 리스트를 제공.
 *    - RouteLocation 클래스의 locationName, homeAddress, distance
 *
 * 드래그로 모달 닫히는 기능 추가하고, 단순 클릭 시에는 키보드만 내리도록 함
 *
 * @param searchResults 검색 결과 리스트
 * @param onItemSelected 리스트 항목 클릭 시 호출되는 콜백 함수
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchResultsModal(
    searchType: LocationType,
    searchResults: List<RouteLocation>,
    onItemSelected: (RouteLocation) -> Unit,
    onDismiss: () -> Unit
) {
    val keyboardController = rememberKeyboardController()

    val scope = rememberCoroutineScope()
    val modalState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Expanded,
        confirmValueChange = { true },
        skipHalfExpanded = true
    )

    // 모달이 닫힐 때(onDismiss) 처리 - 드래그 / 모달 외부 클릭 등 모든 닫힘 이벤트 감지
    LaunchedEffect(modalState.currentValue) {
        if (modalState.currentValue == ModalBottomSheetValue.Hidden) {
            onDismiss()
        }
    }

    ModalBottomSheetLayout(
        sheetState = modalState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        scrimColor = Color.Black.copy(alpha = 0.1f),  // 불투명도 조정
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LocalConfiguration.current.screenHeightDp.dp * 0.7f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        keyboardController.hideKeyboard() // 모달 영역 클릭 시 키보드만 숨김
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 드래그 핸들
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(60.dp)
                        .height(6.dp)
                        .background(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                // 검색 결과 리스트
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = (if (searchType == LocationType.START) "출발지 검색 결과" else "도착지 검색 결과"),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 28.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.secondary,
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (searchResults.isEmpty()) {
                            item {
                                Spacer(modifier = Modifier.padding(top = 24.dp))
                                Text(
                                    text = "검색 결과가 없습니다.",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            items(searchResults) { location ->
                                SearchResultItem(
                                    location = location,
                                    onClick = {
                                        onItemSelected(location)
                                        scope.launch {
                                            modalState.hide() // 드래그로 닫기 대신 프로그래매틱하게 닫기
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { }
}