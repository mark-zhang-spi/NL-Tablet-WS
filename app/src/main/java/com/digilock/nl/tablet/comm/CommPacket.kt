package com.digilock.nl.tablet.comm

import android.util.Log
import com.digilock.nl.tablet.util.*
import com.google.gson.Gson

class CommPacket {
    val packetSize = WS_PACKET_SIZE
    var packetBuffer: ByteArray
    var executeBytes: ExecuteBytes

    init {
        packetBuffer = ByteArray(packetSize)
        executeBytes = ExecuteBytes()
        executeBytes.header = PKT_HEADER
        executeBytes.tail = PKT_TAIL
    }

    fun fromBufferToExecuteBytes() {
        var i = 0
        executeBytes.header = packetBuffer[i++]
        executeBytes.cmd = packetBuffer[i++]
        executeBytes.result = packetBuffer[i++]

        for (x in 0 until PARAMETER_BYTES_SIZE)
            executeBytes.para[x] = packetBuffer[i++]

        executeBytes.tail = packetBuffer[i++]
        executeBytes.crcValue = packetBuffer[i]
    }

    fun fromExecuteBytesToBuffer() {
        var i = 0
        packetBuffer[i++] = executeBytes.header
        packetBuffer[i++] = executeBytes.cmd
        packetBuffer[i++] = executeBytes.result

        for (x in 0 until PARAMETER_BYTES_SIZE)
            packetBuffer[i++] = executeBytes.para[x]

        packetBuffer[i++] = executeBytes.tail
        packetBuffer[i] = executeBytes.crcValue
    }

    fun cleanPacketBuffer(): Boolean {
        for (i in 0 until packetSize)
            packetBuffer[i] = 0x00
        Log.d(LOG_TAG, "cleanPacketBuffer")
        return  true
    }

    companion object {
        private val LOG_TAG: String = CommPacket::class.java.simpleName
        fun clone(source: CommPacket): CommPacket {
            val stringCommonPacket = Gson().toJson(source, CommPacket::class.java)
            return Gson().fromJson<CommPacket>(stringCommonPacket, CommPacket::class.java)
        }
    }


}