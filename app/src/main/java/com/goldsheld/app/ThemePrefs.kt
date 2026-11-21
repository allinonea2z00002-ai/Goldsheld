package com.goldsheld.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Stores the chosen theme: Premium Dark (default), Premium White or follow the system. */
object ThemePrefs {
    private const val FILE = "goldsheld"
    private const val KEY = "night_mode"

    fun get(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY, AppCompatDelegate.MODE_NIGHT_YES)

    fun set(context: Context, mode: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putInt(KEY, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
