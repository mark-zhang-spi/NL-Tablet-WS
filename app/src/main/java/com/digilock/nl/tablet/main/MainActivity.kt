package com.digilock.nl.tablet.main

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.digilock.nl.tablet.App
import com.digilock.nl.tablet.R
import com.digilock.nl.tablet.comm.CommPacket
import com.digilock.nl.tablet.database.dao.*
import com.digilock.nl.tablet.locks.LockInfoActivity
import com.digilock.nl.tablet.users.UserInfoActivity
import com.digilock.nl.tablet.util.*
import com.digilock.nl.tablet.util.constants.*
import com.digilock.nl.tablet.util.custom.ConfirmDialog
import com.digilock.nl.tablet.websocket.*
import com.securitypeople.packagehold.util.scheduler.SchedulerProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.concurrent.scheduleAtFixedRate



class MainActivity: AppCompatActivity(),
        DevicesRecyclerViewAdapter.ItemClickListener,
        MainContract.View {
    override var isActive: Boolean = false

    @Inject lateinit var userDao: UserDao
    @Inject lateinit var lockDao: LockDao
    @Inject lateinit var credentialDao: CredentialDao
    @Inject lateinit var userCredAssignDao: UserCredAssignDao
    @Inject lateinit var userLockAssignDao: UserLockAssignDao
    @Inject lateinit var systemPref: SharedPreferences
    @Inject lateinit var schedulerProvider: SchedulerProvider

    private lateinit var mContext: Context

    override lateinit var presenter: MainContract.Presenter
    private lateinit var dataSource: MainDataSource

    private var btAction = NON_PAIRING_DEVICE

    val timerLockStatus = Timer("update_lock_status_", true)
    var mUpdateTimer = true

    /*
        LOCKS layout related
     */
    private lateinit var recyclerViewLock: RecyclerView
    private lateinit var locksAdapter: LocksRecyclerViewAdapter
    private var spLockFilter: Spinner? = null


    /*
        USERS layout related
     */

    private lateinit var recyclerViewUser: RecyclerView
    private lateinit var usersAdapter: UsersRecyclerViewAdapter
    private var spUserFilter: Spinner? = null

    /*
        REPORTS layout related
    */
    private lateinit var auditFilterLockFuncTypeAdapter: ArrayAdapter<String>

    private lateinit var auditFilterLockNameAdapter: ArrayAdapter<String>
    private var lockNames = ArrayList<String>()

    private lateinit var recyclerViewReports: RecyclerView
    private lateinit var reportsAdapter: ReportsRecyclerViewAdapter

    private lateinit var sharedStatusAdapter: ArrayAdapter<String>

    private lateinit var assignedStatusAdapter: ArrayAdapter<String>

     /*
        Websocket layout related
    */
    private val mDeviceList = arrayListOf<DeviceData>()
    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var devicesAdapter: DevicesRecyclerViewAdapter
    private lateinit var progressBar_BT: ProgressBar



    override fun itemClicked(deviceData: DeviceData) {
        mWsClientService!!.connectServer(deviceData.deviceIPAddress)

        mDeviceList.clear()
        devicesAdapter.notifyDataSetChanged()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mContext = this

        setupViews(savedInstanceState)

        reportsAdapter = ReportsRecyclerViewAdapter(presenter)
        recyclerViewReports.adapter = reportsAdapter
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerViewReports.layoutManager = linearLayoutManager

        locksAdapter = LocksRecyclerViewAdapter(presenter)
        recyclerViewLock.adapter = locksAdapter
        recyclerViewLock.layoutManager = LinearLayoutManager(mContext)

        usersAdapter = UsersRecyclerViewAdapter(this, presenter)
        recyclerViewUser.adapter = usersAdapter
        recyclerViewUser.layoutManager = LinearLayoutManager(mContext)

        presenter.start()
    }

    private fun setupViews(savedInstanceState: Bundle?) {
        (application as App).getNetComponent().inject(this)

        if (!this::dataSource.isInitialized)  dataSource = MainRepository(userDao, lockDao, credentialDao, userCredAssignDao, userLockAssignDao, systemPref)
        if (!this::presenter.isInitialized) presenter = MainPresenter(this, dataSource, schedulerProvider)

        recyclerViewLock = findViewById(R.id.lockRecyclerView_locks)
        recyclerViewUser = findViewById(R.id.userRecyclerView_users)


        /*
            ADMIN layout
         */
        btnUsers_Main.setOnClickListener {
            onShowUsersScreen()
            presenter.showUsersScreenClicked(spUserFilter!!.selectedItemPosition)
        }

        btnLocks_Main.setOnClickListener {
            onShowLocksScreen()
            presenter.showLocksScreenClicked(spLockFilter!!.selectedItemPosition)
        }

        btnReports_Main.setOnClickListener {
            onShowReportsScreen()
        }

        btnSettings_Main.setOnClickListener {
            onShowSettingsScreen()
            presenter.showSettings()
        }

        ivBTConnectStatus_Main.setOnClickListener {
            presenter.connectBleController(this)
        }

        /*
            USERS layout
         */

        btnAddUser_Main.setOnClickListener {
            onAddUser()
        }

        ivAddUser_Main.setOnClickListener {
            onAddUser()
        }

        spUserFilter = this.spFilterUsers_Main
        val aaUserFilter = ArrayAdapter(this, R.xml.spinner_item.toInt(), ARRAY_USER_FILTER)
        aaUserFilter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUserFilter!!.setAdapter(aaUserFilter)

        spUserFilter!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                presenter.filterUsers(p2)
            }
        }

        /*
            LOCKS layout
         */

        btnAddLock_Main.setOnClickListener {
            onAddLock()
        }

        ivAddLock_Main.setOnClickListener {
            onAddLock()
        }

        btnSyncLockStatus_Main.setOnClickListener{
            syncLockStatus()
        }

        spLockFilter = this.spFilterLocks_Main
        val aaLockFilter = ArrayAdapter(this, R.xml.spinner_item.toInt(), ARRAY_LOCK_FILTER)
        aaLockFilter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spLockFilter!!.setAdapter(aaLockFilter)

        spLockFilter!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                presenter.filterLocks(p2)
            }
        }

        /*
            REPORTS layout
         */

        btnCurStatus_Reports.setOnClickListener {
            if(!mWsClientIsConnected) {
                Toast.makeText(this, CONTROLLER_NOT_CONNECTED, Toast.LENGTH_LONG).show()
            } else {
                presenter.doAudit(spLockFuncTypeFilter_Reports.selectedItemPosition, spLockNames_Reports.selectedItemPosition)
                spLockNames_Reports.visibility = View.INVISIBLE
                spLockFuncTypeFilter_Reports.setSelection(AUDIT_LOCK_FUNC_FILTER_ALL.toInt())
            }
        }

        btnSharedStatus_Reports.setOnClickListener {
            if(!mWsClientIsConnected) {
                Toast.makeText(this, CONTROLLER_NOT_CONNECTED, Toast.LENGTH_LONG).show()
            } else {
                presenter.doLockUsageInfo(spSharedStatus_Reports.selectedItemPosition.toByte())
            }

            presenter.sharedStatus(spSharedStatus_Reports.selectedItemPosition)
        }

        btnAssignedStatus_Reports.setOnClickListener{
            presenter.assignedStatus(spAssignedStatus_Reports.selectedItemPosition)
        }

        btnExport_Reports.setOnClickListener{
            checkExternalStorageWritePermission(PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_EXPORT)
        }

        tvLockName_Reports.setOnClickListener{
            presenter.sortByLockName()
        }

        tvLockFunc_Reports.setOnClickListener{
            presenter.sortByLockFunc()
        }

        tvUserName_Reports.setOnClickListener{
            presenter.sortByUserName()
        }

        tvLockAction_Reports.setOnClickListener{
            presenter.sortByLockAction()
        }

        tvAuditDT_Reports.setOnClickListener{
            presenter.sortByAuditDT()
        }

        tvCredType_Reports.setOnClickListener{
            presenter.sortByCredType()
        }

        auditFilterLockFuncTypeAdapter = ArrayAdapter(this, R.xml.spinner_item.toInt(), ARRAY_AUDIT_FILTER)
        spLockFuncTypeFilter_Reports.adapter = auditFilterLockFuncTypeAdapter
        spLockFuncTypeFilter_Reports.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                presenter.selectLockFuncType(p2)
            }
        }

        auditFilterLockNameAdapter = ArrayAdapter(this, R.xml.spinner_item.toInt(), lockNames)
        spLockNames_Reports.adapter = auditFilterLockNameAdapter
        spLockNames_Reports.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                presenter.selectLockName(p2)
            }
        }

        sharedStatusAdapter = ArrayAdapter(this, R.xml.spinner_item.toInt(), ARRAY_SHARED_STATUS_FILTER)
        spSharedStatus_Reports.adapter = sharedStatusAdapter
        spSharedStatus_Reports.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                presenter.sharedStatus(p2)
            }
        }

        assignedStatusAdapter = ArrayAdapter(this, R.xml.spinner_item.toInt(), ARRAY_ASSIGNED_STATUS_FILTER)
        spAssignedStatus_Reports.adapter = assignedStatusAdapter
        spAssignedStatus_Reports.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                presenter.assignedStatus(p2)
            }
        }

        recyclerViewReports = findViewById(R.id.recyclerViewReportRows_Reports)

        /*
            SETTINGS layout
         */
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices_BT)
        recyclerViewDevices.layoutManager = LinearLayoutManager(this)

        devicesAdapter = DevicesRecyclerViewAdapter(mDeviceList = mDeviceList)
        recyclerViewDevices.adapter = devicesAdapter
        devicesAdapter.setItemClickListener(this)

        etIPAddress_Part4_BT.setOnEditorActionListener() { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE) {
                presenter.setControllerIP(this, etIPAddress_Part1_BT.editableText, etIPAddress_Part2_BT.editableText, etIPAddress_Part3_BT.editableText, etIPAddress_Part4_BT.editableText)
                return@setOnEditorActionListener true
            }

            false
            false
        }

        btnSetControllerIP_Settings.setOnClickListener {
            presenter.setControllerIP(this, etIPAddress_Part1_BT.editableText, etIPAddress_Part2_BT.editableText, etIPAddress_Part3_BT.editableText, etIPAddress_Part4_BT.editableText)

            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }

        btnScanController_Settings.setOnClickListener {
            tvPairedBleControllerStatus_BT.text = getString(R.string.no_paired_controller)
            presenter.scanController(this)
        }

        btnSyncController_Settings.setOnClickListener {
            val syncControllerIntent = Intent(this, ConfirmDialog::class.java)
            syncControllerIntent.putExtra(CONFRIM_CODE_PARAMETER, CONFIRM_SYNC_CONTROLLER_CODE)
            startActivityForResult(syncControllerIntent, CONFIRM_SYNC_CONTROLLER)
        }

        btnGetLockStatus_Settings.setOnClickListener {
            if(!mWsClientIsConnected) {
                Toast.makeText(this, CONTROLLER_NOT_CONNECTED, Toast.LENGTH_LONG).show()
            } else {
                presenter.getAllLockStatus()
            }
        }

        btnEmptyAudit_Settings.setOnClickListener {
            val clearAuditTrailIntent = Intent(this, ConfirmDialog::class.java)
            clearAuditTrailIntent.putExtra(CONFRIM_CODE_PARAMETER, CONFIRM_CLEAR_AUDIT_TRAIL_CODE)
            startActivityForResult(clearAuditTrailIntent, CONFIRM_CLEAR_AUDIT_TRAIL)
        }

        /*
            DOWNLOAD DATABASE
         */
        btnDownloadDB_Settings.setOnClickListener{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_DOWNLOAD_DB)
            } else {
                presenter.downloadDB(mContext)
            }
        }

        btnRestoreDB_Settings.setOnClickListener{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_RESTORE_DB)
            } else {
                presenter.restoreDB(mContext)
            }
        }
    }

    override fun showPairedControllerName(name: String) {
        if(name.isNullOrEmpty())    tvPairedBleControllerStatus_BT.text = getString(R.string.no_paired_controller)
        else tvPairedBleControllerStatus_BT.text = name
    }

    override fun connectController(address: String) {
        if (mWsClientService != null) {
            if(mWsClientIsConnected) {
                mWsClientService!!.disconnectServer()
            }

            mWsClientService!!.connectServer(address)
        }
    }

    override fun startScanController() {
        mDeviceList.clear()
        devicesAdapter.notifyDataSetChanged()

        llScanDevice_BT.visibility = View.VISIBLE
    }

    override fun stopScanController() {
        llScanDevice_BT.visibility = View.INVISIBLE
    }

    override fun updateControllerDevicesView(devices: List<DeviceData>) {
        mDeviceList.clear()
        devicesAdapter.notifyDataSetChanged()

        if(devices.size > 0) {
            mDeviceList.addAll(devices)
            devicesAdapter.notifyDataSetChanged()
        }
    }










    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_EXPORT -> {
                presenter.exportReport(mContext,true)
            }
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_DOWNLOAD_DB -> {
                presenter.downloadDB(mContext)
            }
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_RESTORE_DB -> {
                presenter.restoreDB(mContext)
            }
            else -> {

            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

//        progressBar_BT.visibility = View.INVISIBLE

        if(requestCode == ADD_LOCK && resultCode == Activity.RESULT_OK) {
            if(spLockFilter!!.selectedItemPosition == LOCK_FILTER_ALL.toInt()) {
                presenter.filterLocks(LOCK_FILTER_ALL.toInt())
            } else {
                spLockFilter!!.setSelection(LOCK_FILTER_ALL.toInt())
            }
        } else if(requestCode == EDIT_LOCK && resultCode == Activity.RESULT_OK) {
            if(spLockFilter!!.selectedItemPosition == LOCK_FILTER_ALL.toInt()) {
                presenter.filterLocks(LOCK_FILTER_ALL.toInt())
            } else {
                spLockFilter!!.setSelection(LOCK_FILTER_ALL.toInt())
            }
        }else if(requestCode == ADD_USER && resultCode == Activity.RESULT_OK) {
            val userID = data!!.getIntExtra(USER_ID_PARAMETER, 0)
            presenter.updateUserCredPermit(userID)

            spUserFilter!!.setSelection(USER_FILTER_ACTIVE)
        } else if(requestCode == EDIT_USER && resultCode == Activity.RESULT_OK) {
            val userID = data!!.getIntExtra(USER_ID_PARAMETER, 0)
            presenter.updateUserCredPermit(userID)

            spUserFilter!!.setSelection(USER_FILTER_ACTIVE)
        }else if(requestCode == CONFIRM_CHANGE_USER_STATE && resultCode == Activity.RESULT_OK) {
            val userID = data!!.getIntExtra(USER_ID_PARAMETER, 0)
            presenter.changeUserState(userID)
        } else if(requestCode == CONFIRM_CLEAR_AUDIT_TRAIL && resultCode == Activity.RESULT_OK){
            if(!mWsClientIsConnected) {
                Toast.makeText(this, CONTROLLER_NOT_CONNECTED, Toast.LENGTH_LONG).show()
            } else {
                presenter.emptyAudit()
            }
        } else if(requestCode == CONFIRM_SYNC_CONTROLLER && resultCode == Activity.RESULT_OK) {
            if(!mWsClientIsConnected) {
                Toast.makeText(this, CONTROLLER_NOT_CONNECTED, Toast.LENGTH_LONG).show()
            } else {
                presenter.syncController()
            }
        }
    }

    override fun onPause() {
        // Activity paused
        // Stop scan and clear device list, unregister the BroadcastReceiver
        super.onPause()

        if(mWsClientService != null) {
            if(mWsClientIsConnected) {
                mWsClientService!!.disconnectServer()
            }
        }

        unregisterReceiver(mWsClientReceiver)
        unbindService(wsClientConnection)
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter.unsubscribe()

        timerLockStatus.cancel()
    }

    override fun onStart() {
        super.onStart()

        timerLockStatus.scheduleAtFixedRate(100, 300 * 1000) {
            if(mUpdateTimer)    presenter.updateLockStatus()
        }
    }

    override fun onResume() {
        super.onResume()

        wsClientSetFilters()
        startWsClientService(WsClientService::class.java, wsClientConnection, null)
    }

    override fun pauseUpdateTimer() {
        mUpdateTimer = false
    }

    override fun resumeUpdateTimer() {
        mUpdateTimer = true
    }

    private fun hideAllMainScreens() {
        llUsersScreen_Main.visibility = View.GONE
        llLocksScreen_Main.visibility = View.GONE
        llReportsScreen_Main.visibility = View.GONE
        llSettingsScreen_Main.visibility = View.GONE
    }

    private fun onShowUsersScreen() {
        hideAllMainScreens()
        ivUsers_Main.setImageResource(R.mipmap.users_deselected)
        ivLocks_Main.setImageResource(R.mipmap.locks_selected)
        ivReports_Main.setImageResource(R.mipmap.reports_selected)
        ivSettings_Main.setImageResource(R.mipmap.settings_selected)

        //Toggle button display colors - Added by AS
        btnUsers_Main.setBackgroundColor(Color.parseColor("#FFFFFF"))
        tvUsers_Main.setTextColor(Color.parseColor("#13224D"))

        btnLocks_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvLocks_Main.setTextColor(Color.parseColor("#FFFFFF"))

        btnReports_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvReports_Main.setTextColor(Color.parseColor("#FFFFFF"))

        btnSettings_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvSettings_Main.setTextColor(Color.parseColor("#FFFFFF"))
        //End

        llUsersScreen_Main.visibility = View.VISIBLE
    }

    private fun onShowLocksScreen() {
        hideAllMainScreens()
        ivUsers_Main.setImageResource(R.mipmap.users_selected)
        ivLocks_Main.setImageResource(R.mipmap.locks_deselected)
        ivReports_Main.setImageResource(R.mipmap.reports_selected)
        ivSettings_Main.setImageResource(R.mipmap.settings_selected)

        //Toggle button display colors - Added by AS
        btnUsers_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvUsers_Main.setTextColor(Color.parseColor("#FFFFFF"))

        btnLocks_Main.setBackgroundColor(Color.parseColor("#FFFFFF"))
        tvLocks_Main.setTextColor(Color.parseColor("#13224D"))

        btnReports_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvReports_Main.setTextColor(Color.parseColor("#FFFFFF"))

        btnSettings_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvSettings_Main.setTextColor(Color.parseColor("#FFFFFF"))
        //End

        llLocksScreen_Main.visibility = View.VISIBLE
    }

    private fun onShowReportsScreen() {
        hideAllMainScreens()
        ivUsers_Main.setImageResource(R.mipmap.users_selected)
        ivLocks_Main.setImageResource(R.mipmap.locks_selected)
        ivReports_Main.setImageResource(R.mipmap.reports_deselected)
        ivSettings_Main.setImageResource(R.mipmap.settings_selected)

        //Toggle button display colors - Added by AS
        btnUsers_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvUsers_Main.setTextColor(Color.parseColor("#FFFFFF"))

        btnLocks_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvLocks_Main.setTextColor(Color.parseColor("#FFFFFF"))

        btnReports_Main.setBackgroundColor(Color.parseColor("#FFFFFF"))
        tvReports_Main.setTextColor(Color.parseColor("#13224D"))

        btnSettings_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvSettings_Main.setTextColor(Color.parseColor("#FFFFFF"))
        //End

        llReportsScreen_Main.visibility = View.VISIBLE
    }

    private fun onShowSettingsScreen() {
        hideAllMainScreens()
        ivUsers_Main.setImageResource(R.mipmap.users_selected)
        ivLocks_Main.setImageResource(R.mipmap.locks_selected)
        ivReports_Main.setImageResource(R.mipmap.reports_selected)
        ivSettings_Main.setImageResource(R.mipmap.settings_deselected)

        //Toggle button display colors - Added by AS
        btnUsers_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvUsers_Main.setTextColor(Color.parseColor("#FFFFFF"))

        btnLocks_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvLocks_Main.setTextColor(Color.parseColor("#FFFFFF"))

        btnReports_Main.setBackgroundColor(Color.parseColor("#13224D"))
        tvReports_Main.setTextColor(Color.parseColor("#FFFFFF"))

        btnSettings_Main.setBackgroundColor(Color.parseColor("#FFFFFF"))
        tvSettings_Main.setTextColor(Color.parseColor("#13224D"))
        //End

        llSettingsScreen_Main.visibility = View.VISIBLE
    }

    private fun onAddLock() {
        val lockInfoIntent = Intent(this, LockInfoActivity::class.java)
        lockInfoIntent.putExtra(LOCK_ID_PARAMETER, 0)
        startActivityForResult(lockInfoIntent, ADD_LOCK)
    }

    private fun onAddUser() {
        val userInfoIntent = Intent(this, UserInfoActivity::class.java)
        userInfoIntent.putExtra(USER_ID_PARAMETER, 0)
        startActivityForResult(userInfoIntent, ADD_USER)
    }

    /*
     Functions called by presenter
     */

    override fun showErrorMessage(msg: String) {
        Snackbar.make(findViewById(R.id.mainScreen),msg, Snackbar.LENGTH_LONG).show()
    }

    override fun showCommMessage(msg: String) {
        Snackbar.make(findViewById(R.id.mainScreen),msg, Snackbar.LENGTH_SHORT).show()
    }

    override fun sendWsPacket(packet: CommPacket) {
        if(mWsClientService != null ) {
            mWsClientService?.writePacketToServer(packet)
        }
    }


    /*
        USERS layout
     */
    override fun showUsersScreen() {
        if(llUsersScreen_Main.visibility == View.VISIBLE) {
            showDisplayUsersScreen()
            presenter.filterUsers(spUserFilter!!.selectedItemPosition)
        }
    }

    override fun showFilterUsersScreen() {
        if(llUsersScreen_Main.visibility == View.VISIBLE) {
            showDisplayUsersScreen()
            usersAdapter.notifyDataSetChanged()
        }
    }

    override fun showFilterUsersScreen(filterCode: Int) {
        if(llUsersScreen_Main.visibility == View.VISIBLE) {
            spUserFilter!!.setSelection(filterCode)
        }
    }
    override fun showNoUserScreen() {
        if(llUsersScreen_Main.visibility == View.VISIBLE) {
            llDisplayUsersScreen.visibility = View.GONE
            llNoUserScreen.visibility = View.VISIBLE
        }
    }

    private fun showDisplayUsersScreen() {
        llNoUserScreen.visibility = View.GONE
        llDisplayUsersScreen.visibility = View.VISIBLE
    }

    override fun showUserDetail(userID: Int) {
        val userInfoIntent = Intent(this, UserInfoActivity::class.java)
        userInfoIntent.putExtra(USER_ID_PARAMETER, userID)
        startActivityForResult(userInfoIntent, EDIT_USER)
    }

    override fun confirmChangeUserState(userID: Int, disable: Boolean) {
        val confirmChangeUserStateIntent = Intent(this, ConfirmDialog::class.java)
        confirmChangeUserStateIntent.putExtra(CONFRIM_CODE_PARAMETER, CONFIRM_CHANGE_USER_STATE_CODE)
        confirmChangeUserStateIntent.putExtra(USER_ID_PARAMETER, userID)
        confirmChangeUserStateIntent.putExtra(USER_STATE_PARAMETER, disable)
        startActivityForResult(confirmChangeUserStateIntent, CONFIRM_CHANGE_USER_STATE)
    }

    override fun updateChangeUserState() {
        if(llUsersScreen_Main.visibility == View.VISIBLE) {
            presenter.filterUsers(spUserFilter!!.selectedItemPosition)
        }
    }

    /*
        LOCKS layout
     */
    override fun showLocksScreen() {
        if(llLocksScreen_Main.visibility == View.VISIBLE) {
            showDisplayLocksScreen()
            presenter.filterLocks(spLockFilter!!.selectedItemPosition)
        }
    }

    override fun showFilterLocksScreen() {
        if(llLocksScreen_Main.visibility == View.VISIBLE) {
            showDisplayLocksScreen()
            locksAdapter.notifyDataSetChanged()
        }
    }

    override fun showNoLockScreen() {
        if(llLocksScreen_Main.visibility == View.VISIBLE) {
            llDisplayLocksScreen.visibility = View.GONE
            llNoLockScreen.visibility = View.VISIBLE
        }
    }

    private fun showDisplayLocksScreen() {
        llNoLockScreen.visibility = View.GONE
        llDisplayLocksScreen.visibility = View.VISIBLE
    }

    override fun showLockDetail(lockID: Int) {
        val lockInfoIntent = Intent(this, LockInfoActivity::class.java)
        lockInfoIntent.putExtra(LOCK_ID_PARAMETER, lockID)
        startActivityForResult(lockInfoIntent, EDIT_LOCK)
    }


    override fun toggleLockState(lockSN: String) {
        if (mWsClientService != null) {
            if(mWsClientIsConnected) {

            } else {

            }
        } else {

        }

/*
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        // Only if the state is STATE_NONE, do we know that we haven't started already
        if (mCommService?.getState() === BluetoothCommService.STATE_NONE) {
            // Start the Bluetooth chat services
            mCommService?.start()
        }

        if(mCommService?.getState() == BluetoothCommService.STATE_CONNECTED) {
            val packet = presenter.getToggleLockStatePacket(lockSN)
            mCommService?.write(packet.packetBuffer)
        } else {
            Snackbar.make(findViewById(R.id.mainScreen),NOT_CONNECT_CONTROLLER, Snackbar.LENGTH_SHORT).show()
        }
 */
    }

    private fun syncLockStatus() {
/*
        if (mCommService == null) {
            // Initialize the BluetoothChatService to perform bluetooth connections
            mCommService = BluetoothCommService(this, mHandler)
        }

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        // Only if the state is STATE_NONE, do we know that we haven't started already
        if (mCommService?.getState() === BluetoothCommService.STATE_NONE) {
            // Start the Bluetooth chat services
            mCommService?.start()
        }

        if(mCommService?.getState() == BluetoothCommService.STATE_CONNECTED) {
            val packet = presenter.getLockStatusPacket(ALL_LOCKS)
            mCommService?.write(packet.packetBuffer)
        } else {
            Snackbar.make(findViewById(R.id.mainScreen),NOT_CONNECT_CONTROLLER, Snackbar.LENGTH_SHORT).show()
        }
 */
    }

    /*
        Websocket module
     */
    var df: SimpleDateFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    private val mWsClientReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WsClientService.ACTION_WEBSOCKET_CLIENT_CONNECTED -> {
                    Log.e(LOG_TAG, "WebSocket client connected.")
//                    tvServerConnection.text = getString(R.string.server_connected)
                    ivBTConnectStatus_Main.setImageResource(R.mipmap.connected_logo)
                    mWsClientIsConnected = true
                }

                WsClientService.ACTION_WEBSOCKET_CLIENT_DISCONNECTED -> {
                    Log.e(LOG_TAG, "WebSocket client disconnected.")
//                    tvServerConnection.text = getString(R.string.server_disconnected)
                    ivBTConnectStatus_Main.setImageResource(R.mipmap.disconnected_logo)
                    mWsClientIsConnected = false

                    isPingPongTimerCancelled = true
                    Handler().postDelayed ({
                        isPingPongTimerCancelled = false
                        wsPingPongTimer(WS_PING_PONG_PERIOD, WS_PING_PONG_INTERVAL).start()
                    }, WS_PING_PONG_INTERVAL * 2)
                }

                WsClientService.ACTION_WEBSOCKET_CLIENT_FAILURE -> {
                    Log.e(LOG_TAG, "WebSocket client failure.")
//                    tvServerConnection.text = getString(R.string.server_disconnected)
//                    tvServerMessage.text = getString(R.string.server_connect_failure)
                    ivBTConnectStatus_Main.setImageResource(R.mipmap.disconnected_logo)

                    isPingPongTimerCancelled = true
                    Handler().postDelayed ({
                        isPingPongTimerCancelled = false
                        wsPingPongTimer(WS_PING_PONG_PERIOD, WS_PING_PONG_INTERVAL).start()
                    }, WS_PING_PONG_INTERVAL * 2)
                }

                WsClientService.ACTION_WEBSOCKET_CLIENT_DATA_RECEIVED -> {
                    Log.e(LOG_TAG, "WebSocket client received data.")

                    isPingPongTimerCancelled = true
                    Handler().postDelayed ({
                        isPingPongTimerCancelled = false
                        wsPingPongTimer(WS_PING_PONG_PERIOD, WS_PING_PONG_INTERVAL).start()
                    }, WS_PING_PONG_INTERVAL * 2)


                    val msgTime = df.format(Date(System.currentTimeMillis())) + "::"

//                    tvServerMessage.text = ""
                    val cmdType = intent.getStringExtra(JSON_CMD_TYPE)
                    when(cmdType) {
                        CMD_KEEP_CONNECTION -> {
                            val content = intent.getStringExtra(JSON_BODY)
//                            tvServerMessage.text = msgTime + content
                        }
                        CMD_LOCK_CRED_ASSIGNMENT -> {
                            val lockUUID = intent.getStringExtra(LOCK_UUID)
                            val lockUID = intent.getStringExtra(LOCK_UID)
                            val lockFunc = intent.getStringExtra(LOCK_FUNC)
                            val credUUID = intent.getStringExtra(CREDENTIAL_UUID)
                            val credType = intent.getStringExtra(CREDENTIAL_TYPE)
                            val credValue = intent.getStringExtra(CREDENTIAL_VALUE)

//                            presenter.addLockCredAssign(lockUUID, lockUID, lockFunc, credUUID, credType, credValue)

                            mWsClientService!!.replyServer(CMD_LOCK_CRED_ASSIGNMENT, PACKET_SUCCESS)
                        }
                        CMD_LOCK_CRED_DEASSIGNMENT -> {
                            val lockUUID = intent.getStringExtra(LOCK_UUID)
                            val credUUID = intent.getStringExtra(CREDENTIAL_UUID)
//                            presenter.delLockCredAssign(lockUUID, credUUID)
                        }
                        else -> {
//                            tvServerMessage.text = msgTime + "Unknown Command"
                        }
                    }
                }
            }
        }
    }


    private var mWsClientService: WsClientService? = null
    private var mWsClientIsConnected = false

    private val wsClientConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            mWsClientService = (arg1 as WsClientService.WebSocketBinder).service
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mWsClientService = null
        }
    }

    private var isPingPongTimerCancelled = false
    private fun wsPingPongTimer(period: Long, countDownInterval: Long): CountDownTimer {
        return object: CountDownTimer(period, countDownInterval){
            override fun onTick(millisUntilFinished: Long){
                if (isPingPongTimerCancelled){
                    cancel()
                }
            }

            override fun onFinish() {
                mWsClientService!!.pingServer()
            }
        }
    }

    private fun startWsClientService(service: Class<*>, serviceConnection: ServiceConnection, extras: Bundle?) {
        // Bind to a started service
        if (!WsClientService.CLIENT_SERVICE_CONNECTED) {
            val serviceIntent = Intent(this, service)
            if (extras != null && !extras.isEmpty) {
                val keys = extras.keySet()
                for (key in keys) {
                    val extra = extras.getString(key)
                    serviceIntent.putExtra(key, extra)
                }
            }
            startService(serviceIntent)
        }

        val bindingIntent = Intent(this, service)

        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }


    private fun wsClientSetFilters() {
        val filter = IntentFilter()
        filter.addAction(WsClientService.ACTION_WEBSOCKET_CLIENT_CONNECTED)
        filter.addAction(WsClientService.ACTION_WEBSOCKET_CLIENT_DISCONNECTED)
        filter.addAction(WsClientService.ACTION_WEBSOCKET_CLIENT_FAILURE)
        filter.addAction(WsClientService.ACTION_WEBSOCKET_CLIENT_DATA_RECEIVED)
        registerReceiver(mWsClientReceiver, filter)
    }



    /*
        REPORTS
     */
    override fun showLockNamesFilter(hide: Boolean) {
        if(hide)    spLockNames_Reports.visibility = View.INVISIBLE
        else {
            spLockNames_Reports.visibility = View.VISIBLE
            spLockNames_Reports.setSelection(0)
            presenter.selectLockName(0)
        }
    }

    override fun showAuditTrail(hide: Boolean) {
        when(hide) {
            true -> llAuditTrails_Reports.visibility = View.INVISIBLE
            else -> {
                tvName_Reports.text = TITLE_AUDIT_TRAIL
                rdgpExport_Reports.check(R.id.rdbtnInternalMem_Reports)
                llAuditTrails_Reports.visibility = View.VISIBLE

                tvUserName_Reports.visibility = View.VISIBLE
                vspUserName_reports.visibility = View.VISIBLE

                tvLockAction_Reports.visibility = View.VISIBLE
                vspLockAction_reports.visibility = View.VISIBLE

                tvAuditDT_Reports.visibility = View.VISIBLE
                vspAuditDT_reports.visibility = View.VISIBLE

                tvCredType_Reports.visibility = View.VISIBLE
                vspCredType_reports.visibility = View.VISIBLE

                tvLockUsers_Reports.visibility = View.GONE
            }
        }
    }

    override fun updateSharedStatusReportCount(locked: Boolean, count: Int) {
        if(locked)  tvName_Reports.text = "${TITLE_SHARED_USE_LOCKED}    Count: ${count}"
        else tvName_Reports.text = "${TITLE_SHARED_USE_UNLOCKED}    Count: ${count}"
    }

    override fun showSharedStatus(hide: Boolean, locked: Boolean, count: Int) {
        when(hide) {
            true -> llAuditTrails_Reports.visibility = View.INVISIBLE
            else -> {
                if(locked)  tvName_Reports.text = "${TITLE_SHARED_USE_LOCKED}    Count: ${count}"
                else tvName_Reports.text = "${TITLE_SHARED_USE_UNLOCKED}    Count: ${count}"
                rdgpExport_Reports.check(R.id.rdbtnInternalMem_Reports)
                llAuditTrails_Reports.visibility = View.VISIBLE

                if(locked) {
                    tvUserName_Reports.visibility = View.VISIBLE
                    vspUserName_reports.visibility = View.VISIBLE

                    tvLockAction_Reports.visibility = View.GONE
                    vspLockAction_reports.visibility = View.GONE

                    tvAuditDT_Reports.visibility = View.VISIBLE
                    vspAuditDT_reports.visibility = View.VISIBLE

                    tvCredType_Reports.visibility = View.VISIBLE
                    vspCredType_reports.visibility = View.VISIBLE

                    tvLockUsers_Reports.visibility = View.GONE
                } else {
                    tvUserName_Reports.visibility = View.INVISIBLE
                    vspUserName_reports.visibility = View.INVISIBLE

                    tvLockAction_Reports.visibility = View.INVISIBLE
                    vspLockAction_reports.visibility = View.INVISIBLE

                    tvAuditDT_Reports.visibility = View.INVISIBLE
                    vspAuditDT_reports.visibility = View.INVISIBLE

                    tvCredType_Reports.visibility = View.INVISIBLE
                    vspCredType_reports.visibility = View.INVISIBLE

                    tvLockUsers_Reports.visibility = View.GONE
                }
            }
        }
    }

    override fun showAssignedStatus(hide: Boolean, hasUser: Boolean, count: Int) {
        when(hide) {
            true -> llAuditTrails_Reports.visibility = View.INVISIBLE
            else -> {
                if(hasUser)  tvName_Reports.text = "${TITLE_ASSIGNED_USE_HAS_USER}    Count: ${count}"
                else tvName_Reports.text = "${TITLE_ASSIGNED_USE_NO_USER}    Count: ${count}"
                rdgpExport_Reports.check(R.id.rdbtnInternalMem_Reports)
                llAuditTrails_Reports.visibility = View.VISIBLE

                tvUserName_Reports.visibility = View.INVISIBLE
                vspUserName_reports.visibility = View.INVISIBLE

                tvLockAction_Reports.visibility = View.INVISIBLE
                vspLockAction_reports.visibility = View.INVISIBLE

                tvAuditDT_Reports.visibility = View.INVISIBLE
                vspAuditDT_reports.visibility = View.INVISIBLE

                tvCredType_Reports.visibility = View.INVISIBLE
                vspCredType_reports.visibility = View.INVISIBLE

                if(hasUser) tvLockUsers_Reports.visibility = View.VISIBLE
                else tvLockUsers_Reports.visibility = View.GONE
            }
        }
    }

    override fun updateReports() {
        reportsAdapter.notifyDataSetChanged()
    }

    override fun updateReportToEnd() {
        reportsAdapter.notifyDataSetChanged()
        recyclerViewReports.scrollToPosition(recyclerViewReports.adapter.itemCount-1)
    }

    override fun updateLockNames(names: ArrayList<String>) {
        lockNames.clear()
        lockNames.addAll(names)

        auditFilterLockNameAdapter.notifyDataSetChanged()
    }

    private fun checkExternalStorageWritePermission(requestSource: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    requestSource)
        } else {
            when (requestSource) {
                PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_EXPORT -> {
                    presenter.exportReport(mContext, true)
                }

                else -> {
                }
            }
        }
    }

    override fun reStartApp() {
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
        finish()
    }

    companion object {
        private val LOG_TAG: String = MainActivity::class.java.simpleName
        val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_EXPORT = 10
        val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_DOWNLOAD_DB = 11
        val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_RESTORE_DB = 12

        val ADD_LOCK = 101
        val EDIT_LOCK = 102
        val ADD_USER = 103
        val EDIT_USER = 104

        val CONFIRM_CHANGE_USER_STATE = 110
        val CONFIRM_CLEAR_AUDIT_TRAIL = 112
        val CONFIRM_RESTORE_DB = 114
        val CONFIRM_SYNC_CONTROLLER = 115

        val NON_PAIRING_DEVICE = 0x00
        val PAIRING_DEVICE = 0x01

        val WS_PING_PONG_PERIOD = 10 * 60000L
        val WS_PING_PONG_INTERVAL = 100L

    }

}
