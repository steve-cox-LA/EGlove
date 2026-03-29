package com.example.gloveworks30

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class UserInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = UserPreferencesManager(application)

    var firstName by mutableStateOf("")
        private set

    var lastName by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            firstName = preferencesManager.getFirstName()
            lastName = preferencesManager.getLastName()
        }
    }

    fun updateFirstName(newName: String) {
        firstName = newName
        viewModelScope.launch {
            preferencesManager.setFirstName(newName)
        }
    }

    fun updateLastName(newName: String) {
        lastName = newName
        viewModelScope.launch {
            preferencesManager.setLastName(newName)
        }
    }

    fun resetUserInfo() {
        firstName = ""
        lastName = ""
        viewModelScope.launch {
            preferencesManager.clearUserInfo()
        }
    }
}

