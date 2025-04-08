package com.android.reservasiservismobilandroid.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor = prefs.edit()

    companion object {
        private const val PREF_NAME = "ReservasiServisMobilPrefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveUser(userId: Int, name: String, email: String) {
        editor.putInt(KEY_USER_ID, userId)
        editor.putString(KEY_NAME, name)
        editor.putString(KEY_EMAIL, email)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getName(): String = prefs.getString(KEY_NAME, "") ?: ""
    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun clearUser() {
        editor.clear()
        editor.apply()
    }
} 