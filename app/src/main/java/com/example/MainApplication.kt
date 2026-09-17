package com.example

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(
                    this,
                    FirebaseOptions.Builder()
                        .setApplicationId("1:1234567890:android:abcdef1234567890")
                        .setApiKey("placeholder_api_key")
                        .setProjectId("placeholder-project")
                        .build()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
