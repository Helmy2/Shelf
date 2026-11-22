package com.shelf.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.shelf.feature.book.data.local.AndroidBookDatabaseFactory
import com.shelf.shared.app.DreamApp
import com.shelf.shared.di.initKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        initKoin(
            bookDatabaseFactory = AndroidBookDatabaseFactory(applicationContext),
            appContext = applicationContext,
        )
        enableEdgeToEdge()
        setContent {
            DreamApp()
        }
    }
}