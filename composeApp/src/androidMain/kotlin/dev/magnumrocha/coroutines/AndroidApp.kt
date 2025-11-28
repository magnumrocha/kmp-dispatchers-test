package dev.magnumrocha.coroutines

import android.app.Application
import android.content.Context

class AndroidApp : Application() {
    companion object {
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}
