package com.goldsheld.app

import android.content.Context
import com.goldsheld.app.data.EventLog
import com.goldsheld.app.data.StatsRepository
import com.goldsheld.app.data.blocklist.BlocklistManager
import com.goldsheld.app.data.db.AppDatabase
import com.goldsheld.app.data.prefs.SecureStore
import com.goldsheld.app.data.prefs.SettingsStore
import com.goldsheld.app.domain.LockManager
import com.goldsheld.app.location.GeofenceManager
import com.goldsheld.app.security.IntegrityChecker
import com.goldsheld.app.service.UsageRepository
import com.goldsheld.app.util.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Tiny service locator. Created once in [GoldsheldApp.onCreate], before any service, receiver or
 * worker runs, so every component can use the same database and engine.
 */
object Graph {
    private lateinit var appContext: Context

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val db: AppDatabase by lazy { AppDatabase.get(appContext) }
    val settings: SettingsStore by lazy { SettingsStore(appContext) }
    val secure: SecureStore by lazy { SecureStore(appContext) }
    val notifier: Notifier by lazy { Notifier(appContext) }
    val stats: StatsRepository by lazy { StatsRepository(db.statsDao()) }
    val eventLog: EventLog by lazy { EventLog(db.securityEventDao()) }
    val blocklist: BlocklistManager by lazy { BlocklistManager(appContext, db) }
    val geofences: GeofenceManager by lazy { GeofenceManager(appContext) }
    val usage: UsageRepository by lazy { UsageRepository(appContext) }
    val lockManager: LockManager by lazy {
        LockManager(appContext, db, stats, eventLog, notifier, settings, geofences)
    }
    val integrity: IntegrityChecker by lazy {
        IntegrityChecker(appContext, db, settings, eventLog, stats, notifier, usage)
    }
}
