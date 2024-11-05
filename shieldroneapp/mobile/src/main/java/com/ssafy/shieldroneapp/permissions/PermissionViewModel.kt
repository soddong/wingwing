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

    init {
        _audioPermissionGranted.value =
            permissionManager.hasAudioPermission()
    }

    fun updateAudioPermissionStatus(granted: Boolean) {
        _audioPermissionGranted.value = granted
    }
}
