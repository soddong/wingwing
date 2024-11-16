package com.ssafy.shieldroneapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.shieldroneapp.data.model.AlertData
import com.ssafy.shieldroneapp.data.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
) : ViewModel() {

    val currentAlert: StateFlow<AlertData?> = alertRepository.currentAlert

    private val _confirmedFromMobile = MutableStateFlow(false)
    val confirmedFromMobile: StateFlow<Boolean> = _confirmedFromMobile.asStateFlow()

    val isSafeConfirmed: StateFlow<Boolean> = alertRepository.isSafeConfirmed

    fun clearAlert() {
        viewModelScope.launch {
            alertRepository.clearAlert()
            alertRepository.updateSafeConfirmation(false)
            _confirmedFromMobile.value = false
        }
    }
}