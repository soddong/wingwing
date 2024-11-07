package com.ssafy.shieldroneapp.ui.authentication

/**
 * 인증 단계를 정의하는 sealed class
 */
sealed class AuthStep {
    object Intro : AuthStep()
    object Name : AuthStep()
    object Birth : AuthStep()
    object Phone : AuthStep()
    object Verification : AuthStep()
    object Terms : AuthStep()
    object Complete : AuthStep()
}