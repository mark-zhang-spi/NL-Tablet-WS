package com.securitypeople.packagehold.util.scheduler

import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

/**
 * - ImmediateSchedulerProvider implementation of [BaseSchedulerProvider]
 * making all [Schedulers] execute synchronously for assertion on the unit test
 */
class ImmediateSchedulerProvider: BaseSchedulerProvider {

    override fun computation(): Scheduler {
        return Schedulers.trampoline()
    }

    override fun io(): Scheduler {
        return Schedulers.trampoline()
    }

    override fun ui(): Scheduler {
        return Schedulers.trampoline()
    }
}