package com.digilock.nl.tablet.comm


import com.digilock.nl.tablet.util.PARAMETER_BYTES_SIZE


class ExecuteBytes {
    var header: Byte = 0
    var cmd: Byte = 0
    var result: Byte = 0
    var para: ByteArray = ByteArray(PARAMETER_BYTES_SIZE)
    var tail: Byte = 0
    var crcValue: Byte = 0
}