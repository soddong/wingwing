package com.ssafy.shieldroneapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.shieldroneapp.data.model.AlertData
import com.ssafy.shieldroneapp.data.repository.AlertRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val alertRepository: AlertRepository
) : ViewModel() {
    val currentAlert: StateFlow<AlertData?> = alertRepository.currentAlert

    fun clearAlert() {
        viewModelScope.launch {
            alertRepository.clearAlert()
        }
    }
}