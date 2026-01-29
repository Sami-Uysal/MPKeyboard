package com.samiuysal.keyboard.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MPKeyboardApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
