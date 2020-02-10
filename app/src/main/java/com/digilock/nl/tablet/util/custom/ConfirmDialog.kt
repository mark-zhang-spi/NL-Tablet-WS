package com.digilock.nl.tablet.util.custom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

import android.view.View
import android.view.inputmethod.InputMethodManager
import com.digilock.nl.tablet.R
import com.digilock.nl.tablet.util.*
import com.digilock.nl.tablet.util.constants.*
import kotlinx.android.synthetic.main.activity_confirm_dialog.*

class ConfirmDialog : Activity() {

    private lateinit var mContext: Context
    private var mConfirmDlgCode: Int = 0
    private var mOldUserID: Int = 0
    private var mDisableUser: Boolean = false
    private var mOldLockID: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_dialog)

        mContext = this
        mConfirmDlgCode = intent.extras!!.getInt(CONFRIM_CODE_PARAMETER)

        if(mConfirmDlgCode == CONFIRM_CHANGE_USER_STATE_CODE || mConfirmDlgCode == CONFIRM_DISABLE_USER_CODE) {
            mOldUserID = intent.extras!!.getInt(USER_ID_PARAMETER)
            mDisableUser = intent.extras!!.getBoolean(USER_STATE_PARAMETER)
            if(mDisableUser)    title_ConfirmDlg.text = DISABLE_USER
            else title_ConfirmDlg.text = ENABLE_USER
        } else if(mConfirmDlgCode == CONFIRM_DELETE_LOCK_CODE) {
            mOldLockID = intent.extras!!.getInt(LOCK_ID_PARAMETER)
            title_ConfirmDlg.text = DELETE_LOCK
        } else if(mConfirmDlgCode == CONFIRM_CLEAR_AUDIT_TRAIL_CODE) {
            title_ConfirmDlg.text = CLEAR_AUDIT_TRAIL
        } else if(mConfirmDlgCode == CONFIRM_CLEAR_PAIRED_DEVICE_CODE) {
            title_ConfirmDlg.text = CLEAR_PAIRED_DEVICE
        } else if(mConfirmDlgCode == CONFIRM_SYNC_CONTROLLER_CODE) {
            title_ConfirmDlg.text = SYNC_CONTROLLER
        }

        this.setFinishOnTouchOutside(false)
        setupViews(savedInstanceState)
    }

    private fun setupViews(savedInstanceState: Bundle?) {
        // Need calculate by screen size
        val params = window.attributes
        params.x = -20
        params.height = 300
        params.width = 500
        params.y = -10
        this.window.attributes = params


        etPasscode_ConfirmDlg.setOnClickListener {
            etPasscode_ConfirmDlg.setError(null)
        }

        btn_cancel_ConfirmDlg.setOnClickListener {
            hideKeyboard(etPasscode_ConfirmDlg, true)

            val intent = Intent()
            setResult(RESULT_CANCELED, intent)
            finish()
        }

        btn_done_ConfirmDlg.setOnClickListener{
            if(etPasscode_ConfirmDlg.text.isEmpty()) {
                etPasscode_ConfirmDlg.setError(EMPTY_PASSCODE)
            } else if(!etPasscode_ConfirmDlg.text.toString().equals(DEFAULT_PASSCODE)) {
                etPasscode_ConfirmDlg.setError(NOT_MATCH_PASSCODE)
                etPasscode_ConfirmDlg.selectAll()
            } else {
                hideKeyboard(etPasscode_ConfirmDlg, true)

                val intent = Intent()
                if(mConfirmDlgCode == CONFIRM_CHANGE_USER_STATE_CODE || mConfirmDlgCode == CONFIRM_DISABLE_USER_CODE) {
                    intent.putExtra(USER_ID_PARAMETER, mOldUserID)
                } else if(mConfirmDlgCode == CONFIRM_DELETE_LOCK_CODE) {
                    intent.putExtra(LOCK_ID_PARAMETER, mOldLockID)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        etPasscode_ConfirmDlg.requestFocus()
        hideKeyboard(etPasscode_ConfirmDlg, false)
    }

    private fun hideKeyboard(view: View, hide: Boolean) {
        val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if(hide)
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        else {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}
