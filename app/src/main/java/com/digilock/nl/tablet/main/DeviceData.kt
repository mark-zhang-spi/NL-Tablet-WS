package com.digilock.nl.tablet.main

data class DeviceData(val deviceName: String?, val deviceHardwareAddress: String, val deviceIPAddress: String){

    override fun equals(other: Any?): Boolean {
        val deviceData = other as DeviceData
        return deviceHardwareAddress == deviceData.deviceHardwareAddress
    }

    override fun hashCode(): Int {
        return deviceIPAddress.hashCode()
    }

}