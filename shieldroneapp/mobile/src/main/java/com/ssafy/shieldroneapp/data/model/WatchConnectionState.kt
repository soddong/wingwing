package com.ssafy.shieldroneapp.data.model

sealed class WatchConnectionState {
    object Connected : WatchConnectionState()
    object Disconnected : WatchConnectionState()
    data class Error(val message: String) : WatchConnectionState()
    data class Connecting(val message: String = "연결 중...") : WatchConnectionState()
}