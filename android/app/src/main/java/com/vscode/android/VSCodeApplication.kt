package com.vscode.android

import android.app.Application

class VSCodeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Keep application-level setup minimal for M1.
    }
}
