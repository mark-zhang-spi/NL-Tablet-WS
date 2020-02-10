package com.digilock.nl.tablet.users

import android.text.Editable
import com.digilock.nl.tablet.BasePresenter
import com.digilock.nl.tablet.BaseView
import com.digilock.nl.tablet.data.Lock
import com.digilock.nl.tablet.data.User
import com.digilock.nl.tablet.main.AssignLockRowView

interface UserInfoContract {
    interface View: BaseView<Presenter> {
        fun onShowUserInfo(user: User?)
        fun onShowLocks(locks: List<Lock>?)
        fun onShowLockAssignment()

        fun onShowCredAssignment(credAssign: Byte)

        fun onShowErrorMessage(msg: String)
        fun onAddUserComplete(userID: Int)

        fun onShowPrimaryScreen()

        fun onShowWaitCredentialTimeoutMessage(credentialType: Byte)
        fun onShowWaitCredentialExistsMessage(credentialType: Byte)
        fun onShowPinCodeShortMessage()
        fun onShowMobileIDErrorMessage()

        fun scanRFIDCard()
        fun scanMobileID()
        fun enterPinCode()

        fun deleteLastPinCodeEntry(position: Int)
        fun enterAnPinCodeEntry(position: Int, entry: String)
        fun completePinCode()

        fun confirmDisableCurrentUser()

        fun stopAdvertiser()
    }

    interface Presenter: BasePresenter {
        fun lockAssignmentChanged(position: Int, isChecked: Boolean)
        fun addUser(oldUserID: Int, perit: Byte, fstName: Editable?, lstName: Editable?, dept: Editable?, email: Editable?, phone: Editable?, notes: Editable?)
        fun disableUserClicked()
        fun disableUser(userID: Int)

        fun lockCount(): Int
        fun onBindAssignLockRowAtPosition(position: Int, rowView: AssignLockRowView)

        fun cancelWaitCurCredential()
        fun waitCurCredentialAgain()

        fun transferCurCredential()

        fun waitCredentialTimeout()

        fun postRFIDPresent(cardSN: Editable)

        fun handlePinCodeKeyActions(newInput: Int, cursorPosition: Int, currentText: String)

        fun postPinCodeDone(pinCode: Editable?)

        fun postMobileIDPresent(mobileID: String?)

        fun updateCredPermit(add: Boolean, cred: Byte)

        fun unsubscribe()
    }
}