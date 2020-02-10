package com.digilock.nl.tablet.locks

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import com.digilock.nl.tablet.R
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.digilock.nl.tablet.App
import com.digilock.nl.tablet.data.Lock
import com.digilock.nl.tablet.database.dao.LockDao
import com.digilock.nl.tablet.database.dao.UserLockAssignDao
import com.digilock.nl.tablet.util.CONFIRM_DELETE_LOCK_CODE
import com.digilock.nl.tablet.util.CREDENTIAL_MOBILEID
import com.digilock.nl.tablet.util.CREDENTIAL_PINCODE
import com.digilock.nl.tablet.util.CREDENTIAL_RFID
import com.digilock.nl.tablet.util.constants.*
import com.digilock.nl.tablet.util.custom.ConfirmDialog
import com.securitypeople.packagehold.util.scheduler.SchedulerProvider
import kotlinx.android.synthetic.main.activity_lock_info.*
import javax.inject.Inject
import kotlin.experimental.and


class LockInfoActivity : Activity(),
        LockInfoContract.View,
        AdapterView.OnItemSelectedListener {

    override var isActive: Boolean = false

    @Inject lateinit var lockDao: LockDao
    @Inject lateinit var userLockAssignDao: UserLockAssignDao
    @Inject lateinit var systemPref: SharedPreferences
    @Inject lateinit var schedulerProvider: SchedulerProvider

    override lateinit var presenter: LockInfoContract.Presenter
    private lateinit var dataSource: LockInfoDataSource


    private var mOldLockId: Int = 0
    private var spLockTypes: Spinner? = null
    private var spLockFuncs: Spinner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_info)

        mOldLockId = intent.extras!!.getInt(LOCK_ID_PARAMETER)

        this.setFinishOnTouchOutside(false)

        setupViews(savedInstanceState)
    }


    private fun setupViews(savedInstanceState: Bundle?) {
        (application as App).getNetComponent().inject(this)

        if (!this::dataSource.isInitialized)  dataSource = LockInfoRepository(lockDao, userLockAssignDao, systemPref)
        if (!this::presenter.isInitialized) presenter = LockInfoPresenter(mOldLockId,this, dataSource, schedulerProvider)

        // Need calculate by screen size
        val params = window.attributes
        params.x = -20
        params.height = 700
        params.width = 500
        params.y = -10
        this.window.attributes = params

        if(mOldLockId > 0) {
            title_LockInfo.text = getString(R.string.edit_lock)
            btnAdd_LockInfo.setText(getString(R.string.button_save))
            btnDelete_LockInfo.visibility = View.VISIBLE
        }

        spLockTypes = this.spLockTypes_LockInfo
        spLockTypes!!.setOnItemSelectedListener(this)

        val aaLockTypes = ArrayAdapter(this, android.R.layout.simple_spinner_item, lockTypes)
        aaLockTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spLockTypes!!.setAdapter(aaLockTypes)

        spLockFuncs = this.spLockFuncs_LockInfo
        spLockFuncs!!.setOnItemSelectedListener(this)

        val aaLockFuncs = ArrayAdapter(this, android.R.layout.simple_spinner_item, lockFuncs)
        aaLockFuncs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spLockFuncs!!.setAdapter(aaLockFuncs)

        btnAdd_LockInfo.setOnClickListener {
            var permit = 0
            if(chbRFID_LockInfo.isChecked)  permit += CREDENTIAL_RFID
            if(chbMobileID_LockInfo.isChecked)  permit += CREDENTIAL_MOBILEID
            if(chbUserPin_LockInfo.isChecked)  permit += CREDENTIAL_PINCODE

            presenter.addLockClicked(mOldLockId, permit.toByte(), lockTypes[spLockTypes_LockInfo.selectedItemPosition], lockFuncs[spLockFuncs_LockInfo.selectedItemPosition], etLockName_LockInfo.text, etLockSN_LockInfo.text, etLocation_LockInfo.text, etDesc_LockInfo.text)
        }

        btnCancel_LockInfo.setOnClickListener {
            val intent = Intent()
            setResult(RESULT_CANCELED, intent)
            finish()
        }

        btnDelete_LockInfo.setOnClickListener {
            presenter.confirmDeleteLockClicked()
        }

        presenter.start()
    }


    override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, id: Long) {
        if(arg0 != null) {
            when(arg0.id) {
                R.id.spLockTypes_LockInfo -> presenter.changeLockType(position)
                else -> {}
            }
        }
    }

    override fun onNothingSelected(arg0: AdapterView<*>) {

    }

    override fun onShowLockInfo(lock: Lock?) {
        if(lock != null) {
            etLockName_LockInfo.setText(lock.lockName)
            etLockSN_LockInfo.setText(lock.lockSN)
            etLocation_LockInfo.setText(lock.lockLocation)
            etDesc_LockInfo.setText(lock.lockNotes)

            chbRFID_LockInfo.isChecked = (lock.credPermit and CREDENTIAL_RFID == CREDENTIAL_RFID)
            chbMobileID_LockInfo.isChecked = (lock.credPermit and CREDENTIAL_MOBILEID == CREDENTIAL_MOBILEID)
            chbUserPin_LockInfo.isChecked = (lock.credPermit and CREDENTIAL_PINCODE == CREDENTIAL_PINCODE)

            spLockFuncs?.setSelection(lockFuncs.indexOf(lock.lockFunc))
            spLockTypes?.setSelection(lockTypes.indexOf(lock.lockType))
        }
    }

    override fun onShowErrorMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onUpdateCredentialPermit(credentialPermit: Byte) {
        chbRFID_LockInfo.isEnabled = (credentialPermit and CREDENTIAL_RFID == CREDENTIAL_RFID)
        if(!chbRFID_LockInfo.isEnabled) chbRFID_LockInfo.isChecked = false

        chbMobileID_LockInfo.isEnabled = (credentialPermit and CREDENTIAL_MOBILEID == CREDENTIAL_MOBILEID)
        if(!chbMobileID_LockInfo.isEnabled) chbMobileID_LockInfo.isChecked = false

        chbUserPin_LockInfo.isEnabled = (credentialPermit and CREDENTIAL_PINCODE == CREDENTIAL_PINCODE)
        if(!chbUserPin_LockInfo.isEnabled)  chbUserPin_LockInfo.isChecked = false
    }

    override fun onAddLockComplete() {
        val intent = Intent()
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun confirmDeleteLock() {
        val confirmDeleteLockIntent = Intent(this, ConfirmDialog::class.java)
        confirmDeleteLockIntent.putExtra(CONFRIM_CODE_PARAMETER, CONFIRM_DELETE_LOCK_CODE)
        confirmDeleteLockIntent.putExtra(LOCK_ID_PARAMETER, mOldLockId)
        startActivityForResult(confirmDeleteLockIntent, CONFIRM_DELETE_LOCK)
    }

    override fun onBackPressed() {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == CONFIRM_DELETE_LOCK && resultCode == Activity.RESULT_OK){
            val lockID = data!!.getIntExtra(LOCK_ID_PARAMETER, 0)
            presenter.deleteLock(lockID)
        }
    }

    companion object {
        private val LOG_TAG: String = LockInfoActivity::class.java.simpleName
        val ADD_LOCK = 1
        val CONFIRM_DELETE_LOCK = 113
    }

}
