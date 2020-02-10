package com.digilock.nl.tablet

import android.app.Application
import com.digilock.nl.tablet.dagger.AppModule
import com.digilock.nl.tablet.dagger.DaggerNetComponent
import com.digilock.nl.tablet.dagger.NetComponent
import com.digilock.nl.tablet.dagger.NetModule
import com.digilock.nl.tablet.util.BASE_URL_SERVER_DEV
import com.facebook.stetho.Stetho
import com.squareup.leakcanary.LeakCanary

/**
 * - App
 */
class App: Application() {

    private lateinit var netComponent: NetComponent

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) return
        LeakCanary.install(this)

        Stetho.initializeWithDefaults(this)
        val url = BASE_URL_SERVER_DEV

        netComponent = DaggerNetComponent.builder()
                .appModule(AppModule(this))
                .netModule(NetModule(url))
                .build()
    }

    fun getNetComponent() = netComponent

}