package com.digilock.nl.tablet.locks

import android.text.Editable
import android.util.Log
import com.digilock.nl.tablet.data.Lock
import com.digilock.nl.tablet.main.MainPresenter
import com.digilock.nl.tablet.util.constants.*
import com.securitypeople.packagehold.util.scheduler.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * - LockInfoPresenter
 */
class LockInfoPresenter(private val oldLockID:Int,
                        private val view: LockInfoContract.View,
                        private val dataSource: LockInfoDataSource,
                        private val scheduler: BaseSchedulerProvider): LockInfoContract.Presenter {

    init {
        view.presenter = this
    }

    private val mLockList = arrayListOf<Lock>()

    private val compositeDisposable = CompositeDisposable()

    override fun start() {
        compositeDisposable.clear()


        // Get all Locks from database
        val obtainLocksDisposable = obtainLocks()
        compositeDisposable.add(obtainLocksDisposable)

        // Get all User-Lock assignments from database
        val obtainUserLockAssignsDisposable = obtainUserLockAssigns()
        compositeDisposable.add(obtainUserLockAssignsDisposable)
    }

    private fun obtainLocks(): Disposable = dataSource.obtainLocks()
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {
                        Log.i(LOG_TAG, "Start: read lock info success .")
                        if(oldLockID != 0) {
                            view.onShowLockInfo(dataSource.getLock(oldLockID))
                        }

                        mLockList.clear()
                        mLockList.addAll(it)

                    },

                    {
                        Log.e(LOG_TAG, "Read lock information from local database failed: \n ${it.localizedMessage}")
                        view.onShowErrorMessage("Error: Read user information from local database failed: \n ${it.localizedMessage}")
                    }
            )


    private fun obtainUserLockAssigns(): Disposable = dataSource.obtainUserLockAssigns()
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {
                        Log.i(LOG_TAG, "Start: read user lock assignment infomation success .")
                    },

                    {
                        Log.e(LOG_TAG, "Read user lock assignment information failed: \n ${it.localizedMessage}")
                        view.onShowErrorMessage("Error: Read user lock assignment information failed: ${it.localizedMessage}")
                    }
            )

    override fun changeLockType(position: Int) {
        view.onUpdateCredentialPermit(credentialPermits[position])
    }

    override fun addLockClicked(oldLockID: Int, permit: Byte, lockType: String, lockFunc: String, lockName: Editable?, lockSN: Editable?, location: Editable?, desc: Editable?) {
        if(lockName!!.isEmpty())  {
            view.onShowErrorMessage(LOCK_NAME_IS_EMPTY)
            return
        }

        if(lockSN!!.isEmpty())  {
            view.onShowErrorMessage(LOCK_SN_IS_EMPTY)
            return
        }

        if(location!!.isEmpty())  {
//            view.onShowErrorMessage(LOCK_LOCATION_IS_EMPTY)
//            return
        }

        if(desc!!.isEmpty())  {
//            view.onShowErrorMessage(LOCK_DESC_IS_EMPTY)
//            return
        }

        if(permit == 0.toByte()) {
            view.onShowErrorMessage(LOCK_CRED_IS_EMPTY)
            return
        }

        if(oldLockID == 0) {
            if(dataSource.lockNameExists(lockName.toString())) {
                view.onShowErrorMessage(LOCK_NAME_EXISTS)
                return
            }

            if(dataSource.lockSNExists(lockSN.toString())) {
                view.onShowErrorMessage(LOCK_SN_EXISTS)
                return
            }

            insertNewLock(permit, lockType, lockFunc, lockName!!.toString(), lockSN!!.toString(), location!!.toString(), desc!!.toString())
        } else {    // For EDIT
            val lock = dataSource.getLock(oldLockID)
            if(!lockName.toString().toLowerCase().equals(lock!!.lockName.toLowerCase())) {  // New LOCK NAME
                if(dataSource.lockNameExists(lockName.toString())) {
                    view.onShowErrorMessage(LOCK_NAME_EXISTS)
                    return
                }
            }

            if(!lockSN.toString().equals(lock!!.lockSN.toString())) {  // New LOCK serial number
                if(dataSource.lockSNExists(lockName.toString())) {
                    view.onShowErrorMessage(LOCK_SN_EXISTS)
                    return
                }
            }

            updateLockInfo(oldLockID, permit, lockType, lockFunc, lockName!!.toString(), lockSN!!.toString(), location!!.toString(), desc!!.toString())
        }
    }

    private fun updateLockInfo(lockId: Int, perit: Byte, lockType: String, lockFunc: String, lockName: String, lockSN: String, location: String, desc: String) {
        val Disposable = dataSource.updateLockInfo(lockId, perit, lockType, lockFunc, lockName, lockSN, location, desc)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {
                            Log.i(LOG_TAG, "Insert lock info success .")
                            view.onAddLockComplete()
                        },

                        {
                            Log.e(LOG_TAG, "Insert lock to local database failed: \n ${it.localizedMessage}")
                            view.onShowErrorMessage("Error: Insert lock to local database failed: \n ${it.localizedMessage}")
                        }
                )

        compositeDisposable.add(Disposable)
    }

    private fun insertNewLock(perit: Byte, lockType: String, lockFunc: String, lockName: String, lockSN: String, location: String, desc: String) {
        val Disposable = dataSource.insertNewLock(perit, lockType, lockFunc, lockName, lockSN, location, desc)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {
                            Log.i(LOG_TAG, "Insert lock info success .")
                            view.onAddLockComplete()
                        },

                        {
                            Log.e(LOG_TAG, "Insert lock to local database failed: \n ${it.localizedMessage}")
                            view.onShowErrorMessage("Error: Insert lock to local database failed: \n ${it.localizedMessage}")
                        }
                )

        compositeDisposable.add(Disposable)
    }

    override fun confirmDeleteLockClicked() {
        view.confirmDeleteLock()
    }

    override fun deleteLock(lockID: Int) {
        var lock: Lock? = null

        mLockList.forEach {
            if(it.lockId == lockID) lock = it
        }
        if(lock == null)    return

        val Disposable = dataSource.deleteLock(lock!!)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {
                            Log.i(LOG_TAG, "Delete lock infomation success .")
                            view.onAddLockComplete()
                        },

                        {
                            Log.e(LOG_TAG, "Delete lock information failed: \n ${it.localizedMessage}")
                            view.onShowErrorMessage("Error: Delete lock information failed: ${it.localizedMessage}")
                        }
                )
        compositeDisposable.add(Disposable)
    }


    override fun unsubscribe() {
        compositeDisposable.clear()
    }

    companion object {
        private val LOG_TAG = LockInfoPresenter::class.java.simpleName
    }

}