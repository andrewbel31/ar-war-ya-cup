package com.andreibelous.arwar.data

import android.content.Context

class PrefsDataSource(
    context: Context
) {

    private val prefs =
        context.getSharedPreferences("${PrefsDataSource::class.java}", Context.MODE_PRIVATE)

    fun getName(): String? =
        prefs.getString(PREFS_NAME_KEY, null)

    fun setName(name: String) {
        prefs.edit().putString(PREFS_NAME_KEY, name).apply()
    }

    private companion object {

        private const val PREFS_NAME_KEY = "PREFS_NAME_KEY"
    }
}