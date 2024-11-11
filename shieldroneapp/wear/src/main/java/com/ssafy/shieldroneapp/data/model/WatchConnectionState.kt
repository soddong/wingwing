package com.ssafy.shieldroneapp.data.model

sealed class WatchConnectionState {
    object Connected : WatchConnectionState()
    object Disconnected : WatchConnectionState()
    object Error : WatchConnectionState()
    data class Connecting(val message: String = "연결 중...") : WatchConnectionState()
}