package com.ssafy.shieldroneapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.shieldroneapp.data.model.WatchConnectionState
import com.ssafy.shieldroneapp.services.connection.MobileConnectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

@HiltViewModel
class HeartRateViewModel @Inject constructor(
    private val connectionManager: MobileConnectionManager
) : ViewModel() {
    private val _watchConnectionState = MutableStateFlow<WatchConnectionState>(WatchConnectionState.Disconnected)
    val watchConnectionState = _watchConnectionState.asStateFlow()

    private val _heartRateData = MutableStateFlow<Int?>(null)
    val heartRateData = _heartRateData.asStateFlow()

    init {
        viewModelScope.launch {
            connectionManager.watchConnectionState.collect { state ->
                _watchConnectionState.emit(state)
            }
        }
    }

    fun updateWatchConnectionState(newState: WatchConnectionState) {
        viewModelScope.launch {
            _watchConnectionState.emit(newState)
        }
    }

    fun updateHeartRate(heartRate: Int) {
        viewModelScope.launch {
            _heartRateData.emit(heartRate)
        }
    }
}