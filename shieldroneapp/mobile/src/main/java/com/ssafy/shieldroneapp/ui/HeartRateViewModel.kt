package com.ssafy.shieldroneapp.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class HeartRateViewModel @Inject constructor() : ViewModel() {
    private val _heartRateData = MutableStateFlow<Boolean?>(null)
    val heartRateData: StateFlow<Boolean?> = _heartRateData.asStateFlow()

    fun updateHeartRate(pulseFlag: Boolean) {
        _heartRateData.value = pulseFlag
    }
}