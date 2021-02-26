package com.codeaudition.sender

import android.app.Application
import com.orhanobut.hawk.Hawk
import timber.log.Timber
import timber.log.Timber.DebugTree


class App:Application() {
    override fun onCreate() {
        super.onCreate()
        Hawk.init(this).build()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }
}