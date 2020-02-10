package com.digilock.nl.tablet.users

import android.text.Editable
import android.util.Log
import com.digilock.nl.tablet.main.AssignLockRowView
import com.digilock.nl.tablet.util.*
import com.digilock.nl.tablet.util.constants.*
import com.securitypeople.packagehold.util.scheduler.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable


class UserInfoPresenter(private val oldUserID:Int,
                        private val view: UserInfoContract.View,
                        private val dataSource: UserInfoDataSource,
                        private val scheduler: BaseSchedulerProvider): UserInfoContract.Presenter {

    init {
        view.presenter = this
    }

    private val compositeDisposable = CompositeDisposable()
    private var transferCredSN = ""
    private var mCurUserID: Int = 0

    override fun start() {
        compositeDisposable.clear()

        // Get all Users from database
        val obtainCredsDisposable = obtainCreds()
        compositeDisposable.add(obtainCredsDisposable)

        // Get all Users from database
        val obtainUsersDisposable = obtainUsers()
        compositeDisposable.add(obtainUsersDisposable)

        // Get all Locks from database
        val obtainLocksDisposable = obtainLocks()
        compositeDisposable.add(obtainLocksDisposable)
    }

    private fun obtainCreds(): Disposable = dataSource.obtainCredentials()
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {
                        Log.i(LOG_TAG, "Start: read credential info success .")
                    },

                    {
                        Log.e(LOG_TAG, "Start: Error: Read credential information failed: \n ${it.localizedMessage}")
                        view.onShowErrorMessage("Start Error: Read credential information failed: \n ${it.localizedMessage}")
                    }
            )

    private fun obtainUsers(): Disposable = dataSource.obtainUsers()
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {
                        Log.i(LOG_TAG, "Start: read user info success .")
                        if(oldUserID != 0) {
                            view.onShowUserInfo(dataSource.getUser(oldUserID))
                        }
                    },

                    {
                        Log.e(LOG_TAG, "Strat Error:  Read user information failed: \n ${it.localizedMessage}")
                        view.onShowErrorMessage("Start Error: Read user information failed: \n ${it.localizedMessage}")
                    }
            )

    private fun obtainLocks(): Disposable = dataSource.obtainLocks()
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {
                        Log.i(LOG_TAG, "Start: read lock info success .")

                        if(oldUserID > 0) {
                            obtainLockAssignsByUserId()
                            obtainCredAssignsByUserId()
                        }
                    },

                    {
                        Log.e(LOG_TAG, "Start Error: Read lock information failed: \n ${it.localizedMessage}")
                        view.onShowErrorMessage("Start Error: Read lock information failed: \n ${it.localizedMessage}")
                    }
            )

    override fun lockCount(): Int {
        Log.i(LOG_TAG, "Adpater lock count: ${dataSource.getPermitLocks().size}")
        return dataSource.getPermitLocks().size
    }

    override fun onBindAssignLockRowAtPosition(position: Int, rowView: AssignLockRowView) {
        val lock = dataSource.getPermitLocks()[position]
        rowView.setLockName(lock.lockName)
        rowView.setUserLockAssignment(dataSource.getLockAssignment(position))
    }

    private fun obtainLockAssignsByUserId(): Disposable = dataSource.obtainLockAssignsByUserId(oldUserID)
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {
                        Log.i(LOG_TAG, "Start: read user lock assignment success .")
                        if(it.size > 0) {
                            view.onShowLockAssignment()
                        }
                    },

                    {
                        Log.e(LOG_TAG, "Start Error: Read user lock assignment failed: \n ${it.localizedMessage}")
                        view.onShowErrorMessage("Start Error: Read user lock assignment failed: \n ${it.localizedMessage}")
                    }
            )

    private fun obtainCredAssignsByUserId(): Disposable = dataSource.obtainCredAssignsByUserId(oldUserID)
            .subscribeOn(scheduler.io())
            .observeOn(scheduler.ui())
            .subscribe(
                    {
                        Log.i(LOG_TAG, "Start: read user credential assignment success .")
                        if(it > 0) {
                            view.onShowCredAssignment(it)
                            dataSource.updatePermitLocks()
                            view.onShowLocks(dataSource.getPermitLocks())
                        }
                    },

                    {
                        Log.e(LOG_TAG, "UserInfoActivity Error: Read user credential assignment failed: \n ${it.localizedMessage}")
                        view.onShowErrorMessage("Start Error: Read user credential assignment failed: \n ${it.localizedMessage}")
                    }
            )


    override fun updateCredPermit(add: Boolean, cred: Byte) {
        dataSource.updateCredPermit(add, cred)
        view.onShowLocks(dataSource.getPermitLocks())
    }


    override fun lockAssignmentChanged(position: Int, isChecked: Boolean) {
        dataSource.updateLockAssignment(position, isChecked)
    }

    override fun addUser(oldUserID: Int, permit: Byte, fstName: Editable?, lstName: Editable?, dept: Editable?, email: Editable?, phone: Editable?, notes: Editable?) {
        if(fstName!!.isEmpty())  {
            view.onShowErrorMessage(USER_FNAME_IS_EMPTY)
            return
        }

        if(lstName!!.isEmpty())  {
            view.onShowErrorMessage(USER_LNAME_IS_EMPTY)
            return
        }

        if(dept!!.isEmpty())  {
//            view.onShowErrorMessage(USER_DEPARTMENT_IS_EMPTY)
//            return
        }

        if(email!!.isEmpty())  {
//            view.onShowErrorMessage(USER_EMAIL_IS_EMPTY)
//            return
        }

        if(phone!!.isEmpty())  {
//            view.onShowErrorMessage(USER_PHONE_IS_EMPTY)
//            return
        }

        if(notes!!.isEmpty())  {
//            view.onShowErrorMessage(USER_NOTES_IS_EMPTY)
//            return
        }

        if(oldUserID == 0) {
            if(dataSource.userNameExists(fstName.toString().toUpperCase(), lstName.toString().toUpperCase())) {
                view.onShowErrorMessage(USER_NAME_EXISTS)
                return
            }

            insertNewUser(permit, fstName!!.toString().toUpperCase(), lstName!!.toString().toUpperCase(), dept!!.toString(), email!!.toString(), phone!!.toString(), notes!!.toString())
        } else {    // For EDIT
            val user = dataSource.getUser(oldUserID)
            if(!(user!!.fstName+user!!.lstName).equals(fstName.toString().toUpperCase()+lstName.toString().toUpperCase())) {  // New USER NAME
                if(dataSource.userNameExists(fstName.toString().toUpperCase(), lstName.toString().toUpperCase())) {
                    view.onShowErrorMessage(USER_NAME_EXISTS)
                    return
                }
            }

            updateUserInfo(oldUserID, permit, fstName!!.toString().toUpperCase(), lstName!!.toString().toUpperCase(), dept!!.toString(), email!!.toString(), phone!!.toString(), notes!!.toString())
        }

    }

    private fun insertNewUser(permit: Byte, fstName: String, lstName: String, dept: String, email: String, phone: String, notes: String) {
        val Disposable = dataSource.insertNewUser(permit, fstName, lstName, dept, email, phone, notes)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {
                            Log.i(LOG_TAG, "Insert user info success .")

                            mCurUserID = it
                            doNextCredential()
                        },

                        {
                            Log.e(LOG_TAG, "Insert user to local database failed: \n ${it.localizedMessage}")
                            view.onShowErrorMessage("Error: Insert user to local database failed: \n ${it.localizedMessage}")
                        }
                )

        compositeDisposable.add(Disposable)
    }

    private fun updateUserInfo(oldUserID: Int, permit: Byte, fstName: String, lstName: String, dept: String, email: String, phone: String, notes: String) {
        val Disposable = dataSource.updateUserInfo(oldUserID, permit, fstName, lstName, dept, email, phone, notes)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {
                            Log.i(LOG_TAG, "Update user info success .")

                            mCurUserID = oldUserID
                            doNextCredential()
                        },

                        {
                            Log.e(LOG_TAG, "Update user to local database failed: \n ${it.localizedMessage}")
                            view.onShowErrorMessage("Error: Update user to local database failed: \n ${it.localizedMessage}")
                        }
                )

        compositeDisposable.add(Disposable)
    }

    private fun addUserComplete() {
        val Disposable = dataSource.updateDatabase()
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {
                            Log.i(LOG_TAG, "Update database success .")

                            view.onAddUserComplete(mCurUserID)
                        },

                        {
                            Log.e(LOG_TAG, "Update database to local database failed: \n ${it.localizedMessage}")
                            view.onShowErrorMessage("Error: Update database to local database failed: \n ${it.localizedMessage}")
                        }
                )

        compositeDisposable.add(Disposable)
    }



    override fun waitCredentialTimeout() {
        view.onShowWaitCredentialTimeoutMessage(dataSource.getWaitedCredentialType())
    }

    override fun cancelWaitCurCredential() {
        if(dataSource.getWaitedCredentialType() == CREDENTIAL_MOBILEID) view.stopAdvertiser()

        dataSource.removeWaitedCredentialType()

        doNextCredential()
    }

    override fun waitCurCredentialAgain() {
        when(dataSource.getWaitedCredentialType()) {
            CREDENTIAL_RFID -> view.scanRFIDCard()
            CREDENTIAL_MOBILEID -> view.scanMobileID()
            CREDENTIAL_PINCODE -> view.enterPinCode()
            else -> {
            }
        }
    }

    override fun transferCurCredential() {
        if(dataSource.getWaitedCredentialType() == CREDENTIAL_MOBILEID) view.stopAdvertiser()

        dataSource.transferCredential(transferCredSN)
        doNextCredential()
    }


    private fun doNextCredential() {
        when (dataSource.getWaitedCredentialType()) {
            CREDENTIAL_RFID -> view.scanRFIDCard()
            CREDENTIAL_MOBILEID -> view.scanMobileID()
            CREDENTIAL_PINCODE -> view.enterPinCode()
            else -> addUserComplete()
        }
    }

    override fun postRFIDPresent(cardSN: Editable) {
        Log.w(LOG_TAG, "RFID CARD SN: ${cardSN}")

        if(dataSource.credAssignExists(cardSN.toString())) {
            transferCredSN = cardSN.toString()
            view.onShowWaitCredentialExistsMessage(dataSource.getWaitedCredentialType())
        } else {
            dataSource.saveCredential(cardSN.toString())
            doNextCredential()
        }
    }

    /*
        PIN CODE
     */
    override fun postPinCodeDone(pinCode: Editable?) {
        if(pinCode!!.toString().length < PIN_CODE_COUNT_MIN) {
            view.onShowPinCodeShortMessage()
        } else {
            var pinSN = ""
            for(c in pinCode.toString()) {
                pinSN += "0" + c.toString()
            }

            dataSource.saveCredential(pinSN)
            doNextCredential()
        }
    }

    override fun handlePinCodeKeyActions(newInput: Int, cursorPosition: Int, currentText: String) {
        var textLength = currentText.length
        when(newInput) {
            // handle deletion
            CODE_DELETE -> {
                if(cursorPosition > 0) {
                    view.deleteLastPinCodeEntry(cursorPosition)
                }
            }
            CODE_DELETE_ALL -> {}
            CODE_FIND -> {}
            CODE_CANCEL -> {}
            CODE_BLANK -> {}
            CODE_OK -> {
                view.completePinCode()
            }
            else -> {
                if (textLength < PIN_CODE_COUNT_MAX) {
                    view.enterAnPinCodeEntry(cursorPosition, newInput.toString())
                } else {
                    Log.e(LOG_TAG, "Need to handle the error. New input received for current text length: $textLength")
                }
            }
        }
    }

    /*
        MOBILE ID
     */
    override fun postMobileIDPresent(mobileID: String?) {
        if(dataSource.getWaitedCredentialType() == CREDENTIAL_MOBILEID) view.stopAdvertiser()

        if(mobileID.isNullOrEmpty()) {
            view.onShowMobileIDErrorMessage()
        } else if(mobileID!!.length < MOBILE_ID_COUNT) {
            view.onShowMobileIDErrorMessage()
        } else {
            if(dataSource.credAssignExists(mobileID.toString())) {
                transferCredSN = mobileID.toString()
                view.onShowWaitCredentialExistsMessage(dataSource.getWaitedCredentialType())
            } else {
                dataSource.saveCredential(mobileID.toString())
                doNextCredential()
            }
        }
    }

    override fun disableUserClicked() {
        view.confirmDisableCurrentUser()
    }

    override fun disableUser(userID: Int) {
        val Disposable = dataSource.disableUser(userID)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.ui())
                .subscribe(
                        {
                            if(it) {
                                Log.i(LOG_TAG, "Disable user success .")
                                view.onAddUserComplete(userID)
                            } else {
                                Log.i(LOG_TAG, "Disable user failed .")
                                view.onShowErrorMessage("Error: Disable user failed")
                            }
                        },

                        {
                            Log.e(LOG_TAG, "Disable user success failed: \n ${it.localizedMessage}")
                            view.onShowErrorMessage("Error: Disable user failed: ${it.localizedMessage}")
                        }
                )
        compositeDisposable.add(Disposable)
    }

    override fun unsubscribe() {
        compositeDisposable.clear()
    }

    companion object {
        private val LOG_TAG = UserInfoPresenter::class.java.simpleName
    }

}