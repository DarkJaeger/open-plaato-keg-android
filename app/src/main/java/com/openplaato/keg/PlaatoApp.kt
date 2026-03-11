package com.openplaato.keg

import android.app.Application
import com.openplaato.keg.notification.PourNotificationHelper
import com.openplaato.keg.notification.PourNotifier
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class PlaatoApp : Application() {

    @Inject lateinit var pourNotifier: PourNotifier

    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        PourNotificationHelper.createChannel(this)
        pourNotifier.start(appScope)
    }
}
