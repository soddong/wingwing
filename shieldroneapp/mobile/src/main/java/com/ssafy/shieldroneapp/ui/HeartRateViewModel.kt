package com.ssafy.shieldroneapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.shieldroneapp.data.model.WatchConnectionState
import com.ssafy.shieldroneapp.data.repository.HeartRateDataRepository
import com.ssafy.shieldroneapp.services.connection.MobileConnectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

@HiltViewModel
class HeartRateViewModel @Inject constructor(
    private val connectionManager: MobileConnectionManager,
) : ViewModel() {

    private val _watchConnectionState = MutableStateFlow<WatchConnectionState>(WatchConnectionState.Disconnected)
    val watchConnectionState = _watchConnectionState.asStateFlow()

    private val _heartRateData = MutableStateFlow(0.0)
    val heartRateData = _heartRateData.asStateFlow()

    fun updateWatchConnectionState(newState: WatchConnectionState) {
        viewModelScope.launch {
            _watchConnectionState.emit(newState)
        }
    }

    fun updateHeartRate(bpm: Double) {
        viewModelScope.launch {
            try {
                _heartRateData.emit(bpm)
            } catch (e: Exception) {
                Log.e("HeartRateViewModel", "심박수 업데이트 실패", e)
            }
        }
    }

    // 뷰모델이 소멸될 때 리소스 정리
    override fun onCleared() {
        super.onCleared()
    }
}