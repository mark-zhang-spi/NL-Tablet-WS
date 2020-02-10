package com.securitypeople.packagehold.util.scheduler

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * - SchedulerProvider implementation class for [BaseSchedulerProvider]
 */
object SchedulerProvider: BaseSchedulerProvider {


    override fun computation(): Scheduler = Schedulers.computation()

    override fun io(): Scheduler = Schedulers.io()

    override fun ui(): Scheduler = AndroidSchedulers.mainThread()
}