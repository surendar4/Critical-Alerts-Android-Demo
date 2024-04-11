package com.surendar4.criticalnotifications

import android.app.Application

class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        instance = this
    }

    companion object {
        private var instance: BaseApplication? = null

        fun getInstance(): BaseApplication {
            return instance!!
        }
    }
}