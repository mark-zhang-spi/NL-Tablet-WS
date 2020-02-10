package com.digilock.nl.tablet.main

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.digilock.nl.tablet.R
import com.digilock.nl.tablet.users.UserInfoContract
import kotlinx.android.synthetic.main.item_assign_lock_row.view.*



class AssignLocksRecyclerViewAdapter(private val presenter: UserInfoContract.Presenter): RecyclerView.Adapter<AssignLocksRecyclerViewAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent!!.context)
        val view = inflater.inflate(R.layout.item_assign_lock_row, parent, false)
        return ViewHolder(view, presenter)
    }


    override fun getItemCount(): Int = presenter.lockCount()

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) = presenter.onBindAssignLockRowAtPosition(position, holder!!)

    companion object {
        val LOG_TAG: String = AssignLocksRecyclerViewAdapter::class.java.simpleName
    }

    class ViewHolder(itemView: View, private val presenter: UserInfoContract.Presenter):
            RecyclerView.ViewHolder(itemView),
            AssignLockRowView,
            View.OnClickListener {

        init {
            itemView.chbAssignState_AssignLockRow.setOnCheckedChangeListener { buttonView, isChecked ->
                presenter.lockAssignmentChanged(adapterPosition, isChecked)
            }
        }

        override fun setLockName(name: String) {
            itemView.tvLockName_AssignLockRow.text = "Lock: $name"
        }

        override fun setUserLockAssignment(isAssigned: Boolean) {
            itemView.chbAssignState_AssignLockRow.isChecked = isAssigned
        }

        override fun onClick(p0: View?) {
            if (p0 != null) {
                when(p0.id) {
                }
            }
        }
    }
}







