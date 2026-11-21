package com.goldsheld.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class GoldsheldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(ThemePrefs.get(this))
    }
}
