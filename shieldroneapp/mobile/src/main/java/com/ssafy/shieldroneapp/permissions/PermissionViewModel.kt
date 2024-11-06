package com.ssafy.shieldroneapp.permissions

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel 
class PermissionViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _audioPermissionGranted = MutableStateFlow(false)
    val audioPermissionGranted: StateFlow<Boolean> = _audioPermissionGranted

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Initial)
    val permissionState: StateFlow<PermissionState> = _permissionState

    init {
        checkInitialPermissions()
    }

    private fun checkInitialPermissions() {
        _audioPermissionGranted.value = permissionManager.hasAudioPermission()
        _permissionState.value = if (_audioPermissionGranted.value) {
            PermissionState.Granted
        } else {
            PermissionState.NotGranted
        }
    }

    fun updateAudioPermissionStatus(granted: Boolean) {
        _audioPermissionGranted.value = granted
        _permissionState.value = if (granted) {
            PermissionState.Granted
        } else {
            PermissionState.Denied
        }
    }
}

sealed class PermissionState {
    object Initial : PermissionState()
    object Granted : PermissionState()
    object NotGranted : PermissionState()
    object Denied : PermissionState()
}