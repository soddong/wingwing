package com.ssafy.shieldroneapp.features.notsupported.ui

import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ssafy.shieldroneapp.core.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.features.heartrate.ui.HeartRateNotSupported

@Composable
fun NotSupportedScreen() {
    HeartRateNotSupported()
}

@Preview(
    device = WearDevices.LARGE_ROUND,
    showSystemUi = true
)

@Composable
private fun NotSupportedScreenPreview() {
    ShieldroneappTheme {
        NotSupportedScreen()
    }
}