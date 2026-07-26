package me.june8th.euet.app

import android.app.Application
import me.june8th.euet.app.di.AppContainer

class EUetApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
