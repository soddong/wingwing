package com.ssafy.shieldroneapp.ui.screens

import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.ui.components.HeartRateNotSupported
import com.ssafy.shieldroneapp.ui.components.SpeedNotSupported

@Composable
fun NotSupportedScreen() {
    HeartRateNotSupported()
    SpeedNotSupported()
}

@Preview(
    device = WearDevices.LARGE_ROUND,
    showSystemUi = true
)

@Composable
private fun NotSupportedScreenPreview() {
    ShieldroneappTheme {
        NotSupportedScreen()
        SpeedNotSupported()
    }
}