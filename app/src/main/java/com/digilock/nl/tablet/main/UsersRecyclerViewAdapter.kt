package com.digilock.nl.tablet.main

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.digilock.nl.tablet.R
import com.digilock.nl.tablet.util.constants.USER_IS_ACTIVE
import com.digilock.nl.tablet.util.constants.USER_IS_DISABLED
import kotlinx.android.synthetic.main.item_lock_row.view.*
import kotlinx.android.synthetic.main.item_user_row.view.*


class UsersRecyclerViewAdapter(context: Context, private val presenter: MainContract.Presenter): RecyclerView.Adapter<UsersRecyclerViewAdapter.ViewHolder>(){
    private var mContext: Context? = null
    init {
        mContext = context
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent!!.context)
        val view = inflater.inflate(R.layout.item_user_row, parent, false)
        return ViewHolder(mContext!!, view, presenter)
    }


    override fun getItemCount(): Int = presenter.userCount()

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) = presenter.onBindUserRowAtPosition(position, holder!!)

    companion object {
        val LOG_TAG: String = UsersRecyclerViewAdapter::class.java.simpleName
    }

    class ViewHolder(context: Context, itemView: View, private val presenter: MainContract.Presenter):
            RecyclerView.ViewHolder(itemView),
            UserRowView,
            View.OnClickListener {

        private var mContext: Context? = null

        init {
            mContext = context
            itemView.ivChangeState_UserRow.setOnClickListener(this)
            itemView.llUserInfo_UserRow.setOnClickListener(this)
        }

        override fun setUserName(name: String) {
            itemView.tvName_UserRow.text = name
        }

        override fun setUserDepartment(dept: String) {
            itemView.tvDept_UserRow.text = dept
        }

        override fun setUserState(state: Boolean) {
            when(state) {
                true -> {
                    itemView.ivChangeState_UserRow.setImageResource(R.mipmap.enable_user_logo)
                    itemView.tvState_UserRow.text = USER_IS_ACTIVE
                }
                else -> {
                    itemView.ivChangeState_UserRow.setImageResource(R.mipmap.disable_user_logo)
                    itemView.tvState_UserRow.text = USER_IS_DISABLED
                }
            }
        }

        override fun setUserDTInfo(dtInfo: String) {
            itemView.tvDTInfo_UserRow.text = dtInfo
        }

        override fun setAssignedLocks(lockNames: List<String>) {
            val aaAssignedLocks = ArrayAdapter(mContext, R.xml.spinner_item, lockNames)
            aaAssignedLocks.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            itemView.spLocks_UserRow.setAdapter(aaAssignedLocks)
        }

        override fun onClick(p0: View?) {
            if (p0 != null) {
                when(p0.id) {
                    R.id.ivChangeState_UserRow -> presenter.confirmChangeUserState(adapterPosition)
                    R.id.llUserInfo_UserRow -> presenter.showUserDetail(adapterPosition)
                }
            }
        }
    }
}







