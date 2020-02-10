package com.digilock.nl.tablet.locks

import android.text.Editable
import com.digilock.nl.tablet.BasePresenter
import com.digilock.nl.tablet.BaseView
import com.digilock.nl.tablet.data.Lock

interface LockInfoContract {
    interface View: BaseView<Presenter> {
        fun onUpdateCredentialPermit(credentialPermit: Byte)
        fun onShowErrorMessage(msg: String)
        fun onAddLockComplete()
        fun onShowLockInfo(lock: Lock?)
        fun confirmDeleteLock()
    }

    interface Presenter: BasePresenter {
        fun changeLockType(position: Int)

        fun addLockClicked(oldLockID: Int, perit: Byte, lockType: String, lockFunc: String, lockName: Editable?, lockSN: Editable?, location: Editable?, desc: Editable?)
        fun confirmDeleteLockClicked()
        fun deleteLock(lockID: Int)

        fun unsubscribe()
    }
}