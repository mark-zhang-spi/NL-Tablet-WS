package com.digilock.nl.tablet.main

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.digilock.nl.tablet.R
import com.digilock.nl.tablet.util.STATE_LOCKED
import com.digilock.nl.tablet.util.STATE_UNLOCKED
import com.digilock.nl.tablet.util.constants.LOCK_IS_LOCKED
import com.digilock.nl.tablet.util.constants.LOCK_IS_UNKNOWN
import com.digilock.nl.tablet.util.constants.LOCK_IS_UNLOCKED
import kotlinx.android.synthetic.main.item_lock_row.view.*


class LocksRecyclerViewAdapter(private val presenter: MainContract.Presenter): RecyclerView.Adapter<LocksRecyclerViewAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent!!.context)
        val view = inflater.inflate(R.layout.item_lock_row, parent, false)
        return ViewHolder(view, presenter)
    }


    override fun getItemCount(): Int = presenter.lockCount()

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) = presenter.onBindLockRowAtPosition(position, holder!!)

    companion object {
        val LOG_TAG: String = LocksRecyclerViewAdapter::class.java.simpleName
    }

    class ViewHolder(itemView: View, private val presenter: MainContract.Presenter):
            RecyclerView.ViewHolder(itemView),
            LockRowView,
            View.OnClickListener {

        init {
//            itemView.ivCancel_LockRow.setOnClickListener(this)
            itemView.llLockInfo_LockRow.setOnClickListener(this)
            itemView.ivState_LockRow.setOnClickListener(this)
        }

        override fun setLockName(name: String) {
            itemView.tvName_LockRow.text = name
        }

        override fun setLockState(state: Byte) {
            when(state) {
                STATE_UNLOCKED -> {
                    itemView.tvState_LockRow.text = LOCK_IS_UNLOCKED
                    itemView.ivState_LockRow.setImageResource(R.mipmap.unlocked_logo)
                }
                STATE_LOCKED -> {
                    itemView.tvState_LockRow.text = LOCK_IS_LOCKED
                    itemView.ivState_LockRow.setImageResource(R.mipmap.locked_logo)
                }
                else -> {
                    itemView.tvState_LockRow.text = LOCK_IS_UNKNOWN
                    itemView.ivState_LockRow.setImageResource(R.mipmap.unknownlock_logo)
                }
            }
        }

        override fun onClick(p0: View?) {
            if (p0 != null) {
                when(p0.id) {
//                    R.id.ivCancel_LockRow -> presenter.confirmDeleteLock(adapterPosition)
                    R.id.llLockInfo_LockRow -> presenter.showLockDetail(adapterPosition)
                    R.id.ivState_LockRow -> presenter.toggleLockState(adapterPosition)
                }
            }
        }
    }
}







