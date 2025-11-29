package com.dmood.app

import android.app.Application
import com.dmood.app.di.DmoodServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DmoodApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        DmoodServiceLocator.init(applicationContext)

        applicationScope.launch {
            DmoodServiceLocator.reminderScheduler.syncReminders()
        }
    }
}
