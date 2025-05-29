package com.example.gloveworks30

import android.content.Context
import android.content.SharedPreferences

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun getFirstName(): String {
        return prefs.getString("first_name", "") ?: ""
    }

    fun getLastName(): String {
        return prefs.getString("last_name", "") ?: ""
    }

    fun setFirstName(name: String) {
        prefs.edit().putString("first_name", name).apply()
    }

    fun setLastName(name: String) {
        prefs.edit().putString("last_name", name).apply()
    }

    fun clearUserInfo() {
        prefs.edit().remove("first_name").remove("last_name").apply()
    }
}
