package com.digilock.nl.tablet.dagger

import com.digilock.nl.tablet.locks.LockInfoActivity
import com.digilock.nl.tablet.main.MainActivity
import com.digilock.nl.tablet.users.UserInfoActivity
import dagger.Component
import javax.inject.Singleton

/**
 * - NetComponent for assigning the references to the activities and fragments defined in [NetModule]
 */
@Singleton
@Component(dependencies = [], modules = [(AppModule::class), (NetModule::class)])
interface NetComponent {

    fun inject(mainActivity: MainActivity)
    fun inject(lockInfoActivity: LockInfoActivity)
    fun inject(userInfoActivity: UserInfoActivity)

}