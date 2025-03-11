package com.example.myapplication

import android.app.Application
import com.example.myapplication.util.ConfigUtil

/**
 * Custom Application class for application-wide initialization
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize config utility with application context
        ConfigUtil.init(applicationContext)
    }
}
