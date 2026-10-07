package com.devos.ai.feature.auth.login

sealed class LoginNavEvent {
    data object ToHome : LoginNavEvent()
}
