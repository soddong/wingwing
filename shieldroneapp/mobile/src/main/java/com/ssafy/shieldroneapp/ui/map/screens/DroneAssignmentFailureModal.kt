package com.ssafy.shieldroneapp.ui.map.screens

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable

@Composable
fun DroneAssignmentFailureModal(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("드론 배정 실패") },
        text = { Text(errorMessage) },
        confirmButton = {
            TextButton (onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}
