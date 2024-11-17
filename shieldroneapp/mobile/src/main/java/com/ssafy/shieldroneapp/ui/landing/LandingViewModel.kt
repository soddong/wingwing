package com.ssafy.shieldroneapp.ui.landing

import androidx.lifecycle.ViewModel
import com.ssafy.shieldroneapp.services.manager.AudioServiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val audioServiceManager: AudioServiceManager
) : ViewModel() {
    fun startAudioService() {
        audioServiceManager.startAudioService()
    }
}