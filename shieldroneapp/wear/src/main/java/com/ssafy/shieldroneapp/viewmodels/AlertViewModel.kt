package com.ssafy.shieldroneapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.AlertData
import com.ssafy.shieldroneapp.data.repository.AlertRepository
import com.ssafy.shieldroneapp.utils.await
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        private const val TAG = "워치: 알림 뷰모델"
        private const val PATH_SAFE_CONFIRMATION = "/watch_safe_confirm"
    }

    val currentAlert: StateFlow<AlertData?> = alertRepository.currentAlert

    private val _confirmedFromMobile = MutableStateFlow(false)
    val confirmedFromMobile: StateFlow<Boolean> = _confirmedFromMobile.asStateFlow()

    val isSafeConfirmed: StateFlow<Boolean> = alertRepository.isSafeConfirmed

    fun updateConfirmedFromMobile(confirmed: Boolean) {
        _confirmedFromMobile.value = confirmed
    }

    fun clearAlert() {
        viewModelScope.launch {
            alertRepository.clearAlert()
            _confirmedFromMobile.value = false
        }
    }

    fun setSafeConfirmed(isConfirmed: Boolean) {
        viewModelScope.launch {
            alertRepository.updateSafeConfirmation(isConfirmed)
        }
    }
}