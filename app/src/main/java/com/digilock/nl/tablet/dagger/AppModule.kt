package com.digilock.nl.tablet.dagger

import android.app.Application
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * - AppModule
 */
@Module
class AppModule(private val application: Application) {

    @Provides
    @Singleton
    fun providesContext() = application
}