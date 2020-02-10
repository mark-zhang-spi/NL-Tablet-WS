package com.digilock.nl.tablet.bluetooth

data class Report (
    var lockName: String = "",
    var lockFunc: String = "",
    var userName: String = "",
    var action: String = "",
    var dateTime: String = "",
    var credType: String = ""
)