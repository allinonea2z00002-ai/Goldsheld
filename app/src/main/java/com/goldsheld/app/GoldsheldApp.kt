package com.goldsheld.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.goldsheld.app.service.WorkScheduler
import kotlinx.coroutines.launch

class GoldsheldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(ThemePrefs.get(this))
        Graph.init(this)
        Graph.notifier.createChannels()
        WorkScheduler.schedulePeriodic(this)

        Graph.appScope.launch {
            // Load the filter lists and bring the lock state up to date after any process restart.
            Graph.blocklist.reload()
            Graph.lockManager.checkpoint()
        }
    }
}
