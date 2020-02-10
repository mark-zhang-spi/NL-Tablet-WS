package com.digilock.nl.tablet.users

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Bundle
import android.os.CountDownTimer
import android.os.ParcelUuid
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.digilock.nl.tablet.data.User
import com.digilock.nl.tablet.App
import com.digilock.nl.tablet.R
import com.digilock.nl.tablet.data.Lock
import com.digilock.nl.tablet.database.dao.*
import com.digilock.nl.tablet.main.AssignLocksRecyclerViewAdapter
import com.digilock.nl.tablet.main.MainActivity.Companion.CONFIRM_CHANGE_USER_STATE
import com.digilock.nl.tablet.util.*
import com.digilock.nl.tablet.util.constants.*
import com.digilock.nl.tablet.util.custom.ConfirmDialog
import com.securitypeople.packagehold.util.scheduler.SchedulerProvider
import kotlinx.android.synthetic.main.activity_user_info.*
import java.util.*
import javax.inject.Inject
import kotlin.experimental.and

class UserInfoActivity : Activity(),
        KeyboardView.OnKeyboardActionListener,
        UserInfoContract.View{

    override var isActive: Boolean = false

    @Inject lateinit var userDao: UserDao
    @Inject lateinit var lockDao: LockDao
    @Inject lateinit var credDao: CredentialDao
    @Inject lateinit var userLockAssignDao: UserLockAssignDao
    @Inject lateinit var userCredAssignDao: UserCredAssignDao
    @Inject lateinit var systemPref: SharedPreferences
    @Inject lateinit var schedulerProvider: SchedulerProvider

    private lateinit var mContext: Context

    override lateinit var presenter: UserInfoContract.Presenter
    private lateinit var dataSource: UserInfoDataSource

    private lateinit var pinCodeEditable: Editable

    private lateinit var recyclerViewAssignLock: RecyclerView
    private val mAssignLockList = arrayListOf<Lock>()
    private lateinit var assignLocksAdapter: AssignLocksRecyclerViewAdapter

    private var mOldUserId: Int = 0


    /*
        BLE related
     */
    private val DEVICE_IS_TABLET = byteArrayOf(0x7E)

    private val CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
            .fromString("00002901-0000-1000-8000-00805f9b34fb")
    private val CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Nordic Service UUID.  */
    val NORDIC_UUID_SERVICE = UUID.fromString("00001523-1212-efde-1523-785feabcd123")

    /** DDEVICE TYPE characteristic UUID.  */
    private val LBS_UUID_DEVICE_TYPE_CHAR = UUID.fromString("00001524-1212-efde-1523-785feabcd123")

    /** LOCK STATUS + MOBILE ID characteristic UUID.  */
    private val LBS_UUID_LOCK_STATUS_MOBILE_ID_CHAR = UUID.fromString("00001525-1212-efde-1523-785feabcd123")

    private val NORDIC_DIGILOCK_LOCK_STATUS_MOBILE_ID_DESCRIPTION = " Support Nordic Digilock Lock STATUS and Mobile ID."
    private val NORDIC_DIGILOCK_DEVICE_TYPE_DESCRIPTION = " Support Nordic Digilock device type."


    private var mAdvStatus: TextView? = null
    private var mConnectionStatus: TextView? = null
    private var mBluetoothDevices: HashSet<BluetoothDevice>? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mAdvData: AdvertiseData? = null
    private var mAdvScanResponse: AdvertiseData? = null
    private var mAdvSettings: AdvertiseSettings? = null
    private var mAdvertiser: BluetoothLeAdvertiser? = null
    private val mAdvCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(LOG_TAG, "Not broadcasting: $errorCode")
            val statusText: Int
            when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> {
                    statusText = R.string.status_advertising
                    Log.w(LOG_TAG, "App was already advertising")
                }
                ADVERTISE_FAILED_DATA_TOO_LARGE -> statusText = R.string.status_advDataTooLarge
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> statusText = R.string.status_advFeatureUnsupported
                ADVERTISE_FAILED_INTERNAL_ERROR -> statusText = R.string.status_advInternalError
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> statusText = R.string.status_advTooManyAdvertisers
                else -> {
                    statusText = R.string.status_notAdvertising
                    Log.wtf(LOG_TAG, "Unhandled error: $errorCode")
                }
            }
            mAdvStatus!!.setText(statusText)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.v(LOG_TAG, "Broadcasting")
            mAdvStatus!!.setText(R.string.status_advertising)
        }
    }

    private var mGattServer: BluetoothGattServer? = null
    private val mGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBluetoothDevices!!.add(device)
                    updateConnectedDevicesStatus()
                    Log.v(LOG_TAG, "Connected to device: " + device.address)
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevices!!.remove(device)
                    updateConnectedDevicesStatus()
                    Log.v(LOG_TAG, "Disconnected from device")
                }
            } else {
                mBluetoothDevices!!.remove(device)
                updateConnectedDevicesStatus()
                // There are too many gatt errors (some of them not even in the documentation) so we just
                // show the error to the user.
                val errorMessage = getString(R.string.status_errorWhenConnecting) + ": " + status
                runOnUiThread { Toast.makeText(this@UserInfoActivity, errorMessage, Toast.LENGTH_LONG).show() }
                Log.e(LOG_TAG, "Error when connecting: $status")
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                                 characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            Log.d(LOG_TAG, "Device tried to read characteristic: " + characteristic.uuid)
            Log.d(LOG_TAG, "Value: " + Arrays.toString(characteristic.value))
            if (offset != 0) {
                mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
                        /* No need to respond with a value */ null)/* value (optional) */
                return
            }

            mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.value)
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            super.onNotificationSent(device, status)
            Log.v(LOG_TAG, "Notification sent. Status: $status")
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int,
                                                  characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean,
                                                  offset: Int, value: ByteArray) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value)
            Log.v(LOG_TAG, "Characteristic Write request: " + Arrays.toString(value))
            val status = writeCharacteristic(characteristic, offset, value)
            if (responseNeeded) {
                mGattServer!!.sendResponse(device, requestId, status,
                        /* No need to respond with an offset */ 0, null)/* No need to respond with a value */
            }
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int,
                                             offset: Int, descriptor: BluetoothGattDescriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            Log.d(LOG_TAG, "Device tried to read descriptor: " + descriptor.uuid)
            Log.d(LOG_TAG, "Value: " + Arrays.toString(descriptor.value))
            if (offset != 0) {
                mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null)/* value (optional) */
                return
            }
            mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    descriptor.value)
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int,
                                              descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean,
                                              offset: Int,
                                              value: ByteArray) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
                    offset, value)
            Log.v(LOG_TAG, "Descriptor Write Request " + descriptor.uuid + " " + Arrays.toString(value))
            var status = BluetoothGatt.GATT_SUCCESS
            if (descriptor.uuid === CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
                val characteristic = descriptor.characteristic
                val supportsNotifications = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                val supportsIndications = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0

                if (!(supportsNotifications || supportsIndications)) {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                } else if (value.size != 2) {
                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH
                } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS
                    notificationsDisabled(characteristic)
                    descriptor.value = value
                } else if (supportsNotifications && Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS
                    notificationsEnabled(characteristic, false /* indicate */)
                    descriptor.value = value
                } else if (supportsIndications && Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS
                    notificationsEnabled(characteristic, true /* indicate */)
                    descriptor.value = value
                } else {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                }
            } else {
                status = BluetoothGatt.GATT_SUCCESS
                descriptor.value = value
            }
            if (responseNeeded) {
                mGattServer!!.sendResponse(device, requestId, status,
                        /* No need to respond with offset */ 0, null)
            }
        }
    }


    /*
        Lifecycle callbacks
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        mContext = this
        mOldUserId = intent.extras!!.getInt(USER_ID_PARAMETER)

        this.setFinishOnTouchOutside(false)

        setupViews(savedInstanceState)

        /*
            Bluetooth related
         */
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mAdvStatus = findViewById(R.id.tvAdvertisingStatus) as TextView

        mConnectionStatus = findViewById(R.id.tvConnectionStatus) as TextView
        mBluetoothDevices = HashSet()
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.getAdapter()

        // If we are not being restored from a previous state then create and add the fragment.

        initNoricService()
    }

    private lateinit var mNordicMobileIDService: BluetoothGattService
    private lateinit var mDeviceTypeCharacteristic: BluetoothGattCharacteristic
    private lateinit var mLockStatusMobileIDCharacteristic: BluetoothGattCharacteristic

    private fun initNoricService() {
        // DEVICE TYPE characteristic
        mDeviceTypeCharacteristic = BluetoothGattCharacteristic(LBS_UUID_DEVICE_TYPE_CHAR,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ)
        mDeviceTypeCharacteristic.addDescriptor(getClientCharacteristicConfigurationDescriptor())
        mDeviceTypeCharacteristic.addDescriptor(getCharacteristicUserDescriptionDescriptor(NORDIC_DIGILOCK_DEVICE_TYPE_DESCRIPTION))

        mDeviceTypeCharacteristic.value = DEVICE_IS_TABLET


        // LOCK STATUS characteristic
        mLockStatusMobileIDCharacteristic = BluetoothGattCharacteristic(LBS_UUID_LOCK_STATUS_MOBILE_ID_CHAR,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE)
        mLockStatusMobileIDCharacteristic.addDescriptor(getClientCharacteristicConfigurationDescriptor())
        mLockStatusMobileIDCharacteristic.addDescriptor(getCharacteristicUserDescriptionDescriptor(NORDIC_DIGILOCK_LOCK_STATUS_MOBILE_ID_DESCRIPTION))

        mNordicMobileIDService = BluetoothGattService(NORDIC_UUID_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY)

        mNordicMobileIDService.addCharacteristic(mDeviceTypeCharacteristic)
        mNordicMobileIDService.addCharacteristic(mLockStatusMobileIDCharacteristic)


        mAdvSettings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build()

        mAdvData = AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(getServiceUUID())
                .build()

        mAdvScanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()
    }

    private fun setupViews(savedInstanceState: Bundle?) {
        (application as App).getNetComponent().inject(this)

        if (!this::dataSource.isInitialized)  dataSource = UserInfoRepository(userDao, lockDao, credDao, userLockAssignDao, userCredAssignDao, systemPref)
        if (!this::presenter.isInitialized) presenter = UserInfoPresenter(mOldUserId,this, dataSource, schedulerProvider)

        // Need calculate by screen size
        val params = window.attributes
        params.x = -20
        params.height = 700
        params.width = 500
        params.y = -10
        this.window.attributes = params

        if(mOldUserId > 0) {
            title_UserInfo.text = getString(R.string.edit_user)
            btnAdd_UserInfo.setText(getString(R.string.button_save))
            btnDisable_UserInfo.visibility = View.VISIBLE
        }

        chbRFID_UserInfo.setOnCheckedChangeListener{ buttonView, isChecked ->
            presenter.updateCredPermit(isChecked, CREDENTIAL_RFID)
        }

        chbMobileID_UserInfo.setOnCheckedChangeListener{ buttonView, isChecked ->
            presenter.updateCredPermit(isChecked, CREDENTIAL_MOBILEID)
        }

        chbUserPin_UserInfo.setOnCheckedChangeListener{ buttonView, isChecked ->
            presenter.updateCredPermit(isChecked, CREDENTIAL_PINCODE)
        }

        btnAdd_UserInfo.setOnClickListener {
            var permit = 0
            if(chbRFID_UserInfo.isChecked)  permit += CREDENTIAL_RFID
            if(chbMobileID_UserInfo.isChecked)  permit += CREDENTIAL_MOBILEID
            if(chbUserPin_UserInfo.isChecked)  permit += CREDENTIAL_PINCODE

            presenter.addUser(mOldUserId, permit.toByte(), etFstName_UserInfo.text, etLstName_UserInfo.text, etDept_UserInfo.text, etEmail_UserInfo.text, etPhoneNum_UserInfo.text, etNotes_UserInfo.text)
        }

        btnDisable_UserInfo.setOnClickListener {
            presenter.disableUserClicked()
        }

        btnCancel_UserInfo.setOnClickListener {
            val intent = Intent()
            setResult(RESULT_CANCELED, intent)
            finish()
        }

        // Cancel waiting for RFID credential
        btn_cancel_RFID_UserInfo.setOnClickListener{
            isWaitCredentialCancelled = true
        }

        // Cancel waiting for RFID credential
        btn_cancel_PinCode_UserInfo.setOnClickListener{
            isWaitCredentialCancelled = true
        }

        btn_cancel_MobileID_UserInfo.setOnClickListener{
            isWaitCredentialCancelled = true
        }

        // Cancel waiting credentail from timeout
        btn_cancel_wait_credential_UserInfo.setOnClickListener{
            presenter.cancelWaitCurCredential()
        }

        // Waiting credentail from timeout again
        btn_tryagain_wait_credential_UserInfo.setOnClickListener{
            etRFID_SN_UserInfo.text = null
            etPinCode_UserInfo.text = null
            tvMobileID_UserInfo.text = null

            presenter.waitCurCredentialAgain()
        }

        // Transfer credentail from another user
        btn_transfer_wait_credential_UserInfo.setOnClickListener{
            presenter.transferCurCredential()
        }

        recyclerViewAssignLock = findViewById(R.id.recyclerView_userinfo)

        assignLocksAdapter = AssignLocksRecyclerViewAdapter(presenter)
        recyclerViewAssignLock.adapter = assignLocksAdapter
        recyclerViewAssignLock.layoutManager = LinearLayoutManager(mContext)

        kbPinCode_UserInfo.keyboard = Keyboard(mContext, R.xml.keyboard_digits)
        kbPinCode_UserInfo.setOnKeyboardActionListener(this)

        presenter.start()
    }

    fun sendNotificationToDevices(characteristic: BluetoothGattCharacteristic) {
        val indicate = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == BluetoothGattCharacteristic.PROPERTY_INDICATE
        for (device in mBluetoothDevices!!) {
            // true for indication (acknowledge) and false for notification (unacknowledge).
            mGattServer!!.notifyCharacteristicChanged(device, characteristic, indicate)
        }
    }

    private fun resetStatusViews() {
        mAdvStatus!!.setText(R.string.status_notAdvertising)
        updateConnectedDevicesStatus()
    }

    private fun updateConnectedDevicesStatus() {
        val message = (getString(R.string.status_devicesConnected) + " "
                + mBluetoothManager!!.getConnectedDevices(BluetoothGattServer.GATT).size)
        runOnUiThread { mConnectionStatus!!.setText(message) }
    }

    /*
        Bluetooth
     */
    fun getClientCharacteristicConfigurationDescriptor(): BluetoothGattDescriptor {
        val descriptor = BluetoothGattDescriptor(
                CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
        descriptor.value = byteArrayOf(0, 0)
        return descriptor
    }

    fun getCharacteristicUserDescriptionDescriptor(defaultValue: String): BluetoothGattDescriptor {
        val descriptor = BluetoothGattDescriptor(
                CHARACTERISTIC_USER_DESCRIPTION_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
        try {
            descriptor.value = defaultValue.toByteArray(charset("UTF-8"))
        } finally {
            return descriptor
        }
    }

    private fun ensureBleFeaturesAvailable() {
        if (!mBluetoothAdapter!!.isEnabled()) {
            // Make sure bluetooth is enabled.
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun disconnectFromDevices() {
        Log.d(LOG_TAG, "Disconnecting devices...")
        for (device in mBluetoothManager!!.getConnectedDevices(
                BluetoothGattServer.GATT)) {
            Log.d(LOG_TAG, "Devices: " + device.address + " " + device.name)
            mGattServer!!.cancelConnection(device)
        }
    }







    /*
        PIN CODE
     */
    override fun deleteLastPinCodeEntry(position: Int) {
        pinCodeEditable.delete(position - 1, position)
    }

    override fun enterAnPinCodeEntry(position: Int, entry: String) {
        pinCodeEditable.insert(position, entry)
    }

    override fun completePinCode() {
        isPinCodeDone = true
        presenter.postPinCodeDone(etPinCode_UserInfo.text)
    }

    /*
        MOBILE ID
     */
    override fun onShowUserInfo(user: User?) {
        if(user != null) {
            etFstName_UserInfo.setText(user.fstName)
            etLstName_UserInfo.setText(user.lstName)
            etDept_UserInfo.setText(user.dept)
            etEmail_UserInfo.setText(user.email)
            etPhoneNum_UserInfo.setText(user.phoneNum)
            etNotes_UserInfo.setText(user.notes)
        }
    }

    override fun onShowLocks(locks: List<Lock>?) {
        assignLocksAdapter.notifyDataSetChanged()
    }

    override fun onShowLockAssignment() {
        assignLocksAdapter.notifyDataSetChanged()
    }

    override fun onShowCredAssignment(credAssign: Byte) {
        chbRFID_UserInfo.isChecked = (credAssign and CREDENTIAL_RFID == CREDENTIAL_RFID)
        chbMobileID_UserInfo.isChecked = (credAssign and CREDENTIAL_MOBILEID == CREDENTIAL_MOBILEID)
        chbUserPin_UserInfo.isChecked = (credAssign and CREDENTIAL_PINCODE == CREDENTIAL_PINCODE)
    }

    override fun onShowErrorMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onAddUserComplete(userID: Int) {
        val intent = Intent()
        intent.putExtra(USER_ID_PARAMETER, userID)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun hideAllScreens() {
        llPrimary_UserInfo.visibility = View.GONE
        llTEST_UserInfo.visibility = View.GONE
        llWaitRFID_UserInfo.visibility = View.GONE
        llWait_PinCode_UserInfo.visibility = View.GONE
        llWait_MobileID_UserInfo.visibility = View.GONE
    }

    override fun onShowPrimaryScreen() {
        hideAllScreens()
        llPrimary_UserInfo.visibility = View.VISIBLE
    }


    private var isWaitCredentialCancelled = false

    private var prevSN = ""
    override fun scanRFIDCard() {
        hideAllScreens()
        llWaitRFID_UserInfo.visibility = View.VISIBLE

        etRFID_SN_UserInfo.requestFocus()

        isWaitCredentialCancelled = false
        prevSN = ""
        timerRFID(WAIT_CREDENTIAL_PERIOD, 1000).start()
    }

    private fun timerRFID(period:Long, countDownInterval:Long): CountDownTimer {
        return object: CountDownTimer(period, countDownInterval){
            override fun onTick(millisUntilFinished: Long){
                if (isWaitCredentialCancelled){
                    cancel()
                    presenter.cancelWaitCurCredential()
                }

                if(etRFID_SN_UserInfo.text.isNotEmpty()) {
                    if(prevSN.equals(etRFID_SN_UserInfo.text.toString())) {
                        cancel()
                        presenter.postRFIDPresent(etRFID_SN_UserInfo.text)
                    } else {
                        prevSN = etRFID_SN_UserInfo.text.toString()
                    }
                }
            }

            override fun onFinish() {
                presenter.waitCredentialTimeout()
            }
        }
    }


    private var isMobileIDDone = false
    override fun scanMobileID() {
        hideAllScreens()

        isMobileIDDone = false
        startAdvertiser()
        timerMobileID(WAIT_MOBILE_ID_CREDENTIAL_PERIOD, 1000).start()
        llWait_MobileID_UserInfo.visibility = View.VISIBLE
    }

    private fun timerMobileID(period:Long, countDownInterval:Long): CountDownTimer {
        return object: CountDownTimer(period, countDownInterval){
            override fun onTick(millisUntilFinished: Long){
                if (isWaitCredentialCancelled){
                    cancel()
                    presenter.cancelWaitCurCredential()
                }

                if(isMobileIDDone)   {
                    cancel()
                    presenter.postMobileIDPresent(tvMobileID_UserInfo.text.toString())
                }
            }

            override fun onFinish() {
                presenter.waitCredentialTimeout()
            }
        }
    }

    private var isPinCodeDone = false
    override fun enterPinCode() {
        hideAllScreens()

        pinCodeEditable = etPinCode_UserInfo.text
        if(!pinCodeEditable.isNullOrEmpty())    pinCodeEditable.clear()

        isPinCodeDone = false
        timerPinCode(WAIT_CREDENTIAL_PERIOD, 1000).start()
        llWait_PinCode_UserInfo.visibility = View.VISIBLE
    }


    private fun timerPinCode(period:Long, countDownInterval:Long): CountDownTimer {
        return object: CountDownTimer(period, countDownInterval){
            override fun onTick(millisUntilFinished: Long){
                if (isWaitCredentialCancelled){
                    cancel()
                    presenter.cancelWaitCurCredential()
                }

                if(isPinCodeDone)   cancel()
            }

            override fun onFinish() {
                presenter.waitCredentialTimeout()
            }
        }
    }

    override fun onShowWaitCredentialTimeoutMessage(credentialType: Byte) {
        hideAllScreens()

        tvWaitCred_MSG_UserInfo.text = timeoutMsgs()[credentialType]

        llWaitCredential_MSG_UserInfo.visibility = View.VISIBLE
        btn_transfer_wait_credential_UserInfo.visibility = View.GONE
    }

    override fun onShowWaitCredentialExistsMessage(credentialType: Byte) {
        hideAllScreens()

        tvWaitCred_MSG_UserInfo.text = credExistsMsgs()[credentialType]

        llWaitCredential_MSG_UserInfo.visibility = View.VISIBLE
        btn_transfer_wait_credential_UserInfo.visibility = View.VISIBLE
    }

    override fun onShowPinCodeShortMessage() {
        hideAllScreens()

        llWaitCredential_MSG_UserInfo.visibility = View.VISIBLE
        tvWaitCred_MSG_UserInfo.text = MSG_PIN_CODE_IS_SHORT
    }

    override fun onShowMobileIDErrorMessage() {
        hideAllScreens()

        llWaitCredential_MSG_UserInfo.visibility = View.VISIBLE
        tvWaitCred_MSG_UserInfo.text = MSG_MOBILE_ID_IS_ILLEGAL
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        if(llWait_PinCode_UserInfo.visibility == View.VISIBLE) {
            if (!etPinCode_UserInfo.isFocused) etPinCode_UserInfo.requestFocus()

            val position = etPinCode_UserInfo.selectionStart
            pinCodeEditable = etPinCode_UserInfo.text
            presenter.handlePinCodeKeyActions(primaryCode, position, etPinCode_UserInfo.text.toString())
        }
    }

    override fun swipeRight() {}
    override fun onPress(p0: Int) {}
    override fun onRelease(p0: Int) {}
    override fun swipeLeft() {}
    override fun swipeUp() {}
    override fun swipeDown() {}
    override fun onText(p0: CharSequence?) {}

    override fun onBackPressed() {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                if (!mBluetoothAdapter!!.isMultipleAdvertisementSupported()) {
                    Toast.makeText(this, R.string.bluetoothAdvertisingNotSupported, Toast.LENGTH_LONG).show()
                    Log.e(LOG_TAG, "Advertising not supported")
                }
                onStart()
            } else {
                //TODO(g-ortuno): UX for asking the user to activate bt
                Toast.makeText(this, R.string.bluetoothNotEnabled, Toast.LENGTH_LONG).show()
                Log.e(LOG_TAG, "Bluetooth not enabled")
                finish()
            }
        } else if(requestCode == CONFIRM_DISABLE_USER && resultCode == RESULT_OK) {
            val userID = data!!.getIntExtra(USER_ID_PARAMETER, 0)
            presenter.disableUser(userID)
        }
    }


    private fun startAdvertiser() {
        resetStatusViews()

        // If the user disabled Bluetooth when the app was in the background,
        // openGattServer() will return null.
        mGattServer = mBluetoothManager!!.openGattServer(this, mGattServerCallback)
        if (mGattServer == null) {
            ensureBleFeaturesAvailable()
            Toast.makeText(this, GET_GATT_SERVER_FAILED, Toast.LENGTH_SHORT).show()
            return
        }

        // Add a service for a total of three services (Generic Attribute and Generic Access
        // are present by default).
        mGattServer!!.addService(mNordicMobileIDService)

        if (mBluetoothAdapter!!.isMultipleAdvertisementSupported()) {
            mAdvertiser = mBluetoothAdapter!!.getBluetoothLeAdvertiser()
            mAdvertiser!!.startAdvertising(mAdvSettings, mAdvData, mAdvScanResponse, mAdvCallback)
        } else {
            mAdvStatus!!.text = getString(R.string.status_noLeAdv)
        }
    }

    override fun stopAdvertiser() {
        if (mGattServer != null) {
            mGattServer!!.close()
        }

        if (mBluetoothAdapter!!.isEnabled() && mAdvertiser != null) {
            // If stopAdvertising() gets called before close() a null
            // pointer exception is raised.
            mAdvertiser!!.stopAdvertising(mAdvCallback)
        }
    }


    override fun onStop() {
        super.onStop()

        stopAdvertiser()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.unsubscribe()
    }

    private fun getServiceUUID(): ParcelUuid {
        return ParcelUuid(NORDIC_UUID_SERVICE)
    }

    private fun notificationsEnabled(characteristic: BluetoothGattCharacteristic, indicate: Boolean) {
        if (characteristic.uuid !== LBS_UUID_DEVICE_TYPE_CHAR && characteristic.uuid !== LBS_UUID_LOCK_STATUS_MOBILE_ID_CHAR) {
            return
        }

        if (indicate) {
            return
        }

        runOnUiThread {
            Toast.makeText(this, R.string.notificationsEnabled, Toast.LENGTH_SHORT).show()
        }
    }

    private fun notificationsDisabled(characteristic: BluetoothGattCharacteristic) {
        if (characteristic.uuid !== LBS_UUID_DEVICE_TYPE_CHAR && characteristic.uuid !== LBS_UUID_LOCK_STATUS_MOBILE_ID_CHAR) {
            return
        }

        runOnUiThread { Toast.makeText(this, R.string.notificationsNotEnabled, Toast.LENGTH_SHORT).show() }
    }


    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, offset: Int, value: ByteArray): Int {
        val newOffset = value.size


        if (characteristic === mLockStatusMobileIDCharacteristic) {
            if (newOffset == 1) {
                val data: Int = if (value[0] >= 0) value[0].toInt() else 256 + value[0]
                val mobileID = (data shr 1).toByte()
                val sMobileID = String.format("000000%02X", mobileID)
                runOnUiThread { tvMobileID_UserInfo.text = sMobileID }
                isMobileIDDone = true
            }
        }

        return newOffset
    }

    override fun confirmDisableCurrentUser() {
        val confirmDisableUserStateIntent = Intent(this, ConfirmDialog::class.java)
        confirmDisableUserStateIntent.putExtra(CONFRIM_CODE_PARAMETER, CONFIRM_DISABLE_USER_CODE)
        confirmDisableUserStateIntent.putExtra(USER_ID_PARAMETER, mOldUserId)
        confirmDisableUserStateIntent.putExtra(USER_STATE_PARAMETER, true)
        startActivityForResult(confirmDisableUserStateIntent, CONFIRM_DISABLE_USER)
    }

    companion object {
        private val LOG_TAG: String = UserInfoActivity::class.java.simpleName
        val ADD_USER = 1
        val CONFIRM_DISABLE_USER = 2
    }



}
