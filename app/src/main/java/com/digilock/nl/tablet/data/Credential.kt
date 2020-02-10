package com.digilock.nl.tablet.data

import android.annotation.SuppressLint
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.digilock.nl.tablet.util.DB_NAME_CREDS
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = DB_NAME_CREDS)
data class Credential(
        @SerializedName("_sn")
        @ColumnInfo(name = "cred_sn")
        @PrimaryKey
        var credSN: String = "",

        @SerializedName("_type")
        @ColumnInfo(name = "cred_type")
        var credType: Byte = 0                         // 1: RFID, 2: Mobile ID, 4: Pin Code
): Parcelable