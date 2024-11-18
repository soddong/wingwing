package com.ssafy.shieldroneapp.ui.map.screens

import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
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
        title = {
            Text(
                "드론 배정 실패",
                style = MaterialTheme.typography.subtitle1,
            )
        },
        text = {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.body1,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = androidx.compose.material.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colors.secondary
                )
            ) {
                Text(
                    text = "확인",
                    style = MaterialTheme.typography.subtitle2,
                )
            }
        }
    )
}
