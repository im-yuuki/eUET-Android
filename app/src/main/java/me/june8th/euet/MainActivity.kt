package me.june8th.euet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import me.june8th.euet.core.designsystem.theme.EUetTheme
import me.june8th.euet.di.LocalAppContainer
import me.june8th.euet.navigation.EUetApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as EUetApplication).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                EUetTheme {
                    EUetApp()
                }
            }
        }
    }
}
