package com.ssafy.shieldroneapp.ui.map.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.data.model.DroneState
import com.ssafy.shieldroneapp.data.model.response.DroneMatchResponse

@Composable
fun DroneAssignmentSuccessModal(
    droneState: DroneState,
    onDroneCodeInput: (Int) -> Unit,
    onRequestMatching: () -> Unit,
    onDismiss: () -> Unit,
    matchResult: DroneMatchResponse?
) {

    val (droneCode, setDroneCode) = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("드론 배정 성공") },
        text = {
            Column {
                Text("드론 ID: ${droneState.droneId}")
                Text("예상 도착 시간: ${droneState.estimatedTime ?: "N/A"}분")
                Text("거리: ${droneState.distance ?: "N/A"}m")
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = droneCode,
                    onValueChange = { input ->
                        // 숫자만 입력받도록 필터링
                        if (input.all { it.isDigit() }) {
                            setDroneCode(input)
                        }
                    },
                    label = { Text("드론 고유 코드 입력") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                matchResult?.let {
                    Text("매칭 성공! 안전 귀가를 시작합니다.")
                } ?: Text("매칭 대기 중...")
            }
        },
        confirmButton = {
            Button (
                onClick = {
                    // 드론 코드 유효성 검사 추가
                    if (droneCode.isEmpty() || droneCode.toIntOrNull() == null) {
                        Log.e("Map - DroneAssignmentSuccessModal", "Invalid drone code: $droneCode")
                        return@Button
                    }

                    onDroneCodeInput(droneCode.toIntOrNull() ?: 0)
                    onRequestMatching()
                }
            ) {
                Text("드론 매칭 요청")
            }
        },
        dismissButton = {
            TextButton (onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}
