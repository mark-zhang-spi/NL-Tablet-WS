package com.digilock.nl.tablet.main

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.digilock.nl.tablet.R
import com.digilock.nl.tablet.data.Lock
import com.digilock.nl.tablet.util.constants.REPORT_ASSIGNED_USE_HAS_USER
import com.digilock.nl.tablet.util.constants.REPORT_AUDIT_TRAIL
import com.digilock.nl.tablet.util.constants.REPORT_SHARED_USE_LOCKED
import kotlinx.android.synthetic.main.report_row.view.*


class ReportsRecyclerViewAdapter(private val presenter: MainContract.Presenter): RecyclerView.Adapter<ReportsRecyclerViewAdapter.ViewHolder>(){
    val locks = ArrayList<Lock>()
    var mContext: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent!!.context)
        val view = inflater.inflate(R.layout.report_row, parent, false)
        mContext = parent!!.context
        return ViewHolder(view, presenter)
    }

    override fun getItemCount(): Int = presenter.reportRowsCount()

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) = presenter.onBindReportRowAtPosition(mContext!!, position, holder!!)

    companion object {
        val LOG_TAG: String = ReportsRecyclerViewAdapter::class.java.simpleName
    }

    class ViewHolder(itemView: View, private val presenter: MainContract.Presenter):
            RecyclerView.ViewHolder(itemView),
            ReportRowView,
            View.OnClickListener {

        init {

        }

        override fun setIndex(index: String) {
            itemView.tvIndex_ReportRow.text = index
        }

        override fun setLockName(lockName: String) {
            itemView.tvLockName_ReportRow.text = lockName
        }

        override fun setLockFunc(lockFunc: String) {
            itemView.tvLockFunc_ReportRow.text = lockFunc
        }

        override fun setUserName(userName: String) {
            itemView.tvUserName_ReportRow.text = userName
        }

        override fun setAction(action: String) {
            itemView.tvAction_ReportRow.text = action
        }

        override fun setDateTime(dt: String) {
            itemView.tvDateTime_ReportRow.text = dt
        }

        override fun setCredType(credType: String) {
            itemView.tvCredType_ReportRow.text = credType
        }

        override fun setLockUsers(context: Context, lockUsers: Array<String?>) {
            val aaUserFilter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, lockUsers)
            aaUserFilter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            itemView.spLockUsers_ReportRow.setAdapter(aaUserFilter)

        }

        override fun showReportDataItems(reportType: Int) {
            when(reportType.toByte()) {
                REPORT_AUDIT_TRAIL -> {
                    itemView.tvUserName_ReportRow.visibility = View.VISIBLE
                    itemView.vspUserName_ReportRow.visibility = View.VISIBLE

                    itemView.tvAction_ReportRow.visibility = View.VISIBLE
                    itemView.vspAction_ReportRow.visibility = View.VISIBLE

                    itemView.tvDateTime_ReportRow.visibility = View.VISIBLE
                    itemView.vspDateTime_ReportRow.visibility = View.VISIBLE

                    itemView.tvCredType_ReportRow.visibility = View.VISIBLE
                    itemView.vspCredType_ReportRow.visibility = View.VISIBLE

                    itemView.spLockUsers_ReportRow.visibility = View.GONE
                }

                REPORT_SHARED_USE_LOCKED -> {
                    itemView.tvUserName_ReportRow.visibility = View.VISIBLE
                    itemView.vspUserName_ReportRow.visibility = View.VISIBLE

                    itemView.tvAction_ReportRow.visibility = View.GONE
                    itemView.vspAction_ReportRow.visibility = View.GONE

                    itemView.tvDateTime_ReportRow.visibility = View.VISIBLE
                    itemView.vspDateTime_ReportRow.visibility = View.VISIBLE

                    itemView.tvCredType_ReportRow.visibility = View.VISIBLE
                    itemView.vspCredType_ReportRow.visibility = View.VISIBLE

                    itemView.spLockUsers_ReportRow.visibility = View.GONE
                }

                REPORT_ASSIGNED_USE_HAS_USER -> {
                    itemView.tvUserName_ReportRow.visibility = View.INVISIBLE
                    itemView.vspUserName_ReportRow.visibility = View.INVISIBLE

                    itemView.tvAction_ReportRow.visibility = View.INVISIBLE
                    itemView.vspAction_ReportRow.visibility = View.INVISIBLE

                    itemView.tvDateTime_ReportRow.visibility = View.INVISIBLE
                    itemView.vspDateTime_ReportRow.visibility = View.INVISIBLE

                    itemView.tvCredType_ReportRow.visibility = View.INVISIBLE
                    itemView.vspCredType_ReportRow.visibility = View.INVISIBLE

                    itemView.spLockUsers_ReportRow.visibility = View.VISIBLE
                }

                else -> {
                    itemView.tvUserName_ReportRow.visibility = View.INVISIBLE
                    itemView.vspUserName_ReportRow.visibility = View.INVISIBLE

                    itemView.tvAction_ReportRow.visibility = View.INVISIBLE
                    itemView.vspAction_ReportRow.visibility = View.INVISIBLE

                    itemView.tvDateTime_ReportRow.visibility = View.INVISIBLE
                    itemView.vspDateTime_ReportRow.visibility = View.INVISIBLE

                    itemView.tvCredType_ReportRow.visibility = View.INVISIBLE
                    itemView.vspCredType_ReportRow.visibility = View.INVISIBLE

                    itemView.spLockUsers_ReportRow.visibility = View.GONE
                }

            }
        }

        override fun onClick(p0: View?) {
            if (p0 != null) {
                when(p0.id) {
                }
            }
        }
    }
}







