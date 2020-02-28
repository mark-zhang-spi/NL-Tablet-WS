package com.digilock.nl.tablet.websocket

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.digilock.nl.tablet.comm.CommPacket
import com.digilock.nl.tablet.util.packetToString
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.nio.ByteBuffer

class WsClientService : Service() {
    private val binder: IBinder = WebSocketBinder()
    private var context: Context? = null

    private var mWebSocketClient: OkHttpClient? = null
    private var mWebSocket: WebSocket? = null
    private var mIsServerConnected = false
    private var mAllowAutoConnect = true

    private var urlServer = "ws://nl-server-testing.herokuapp.com"
    private var sysToken = "0123456789"


    override fun onCreate() {
        context = this
        CLIENT_SERVICE_CONNECTED = true
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        CLIENT_SERVICE_CONNECTED = false
    }

    inner class WebSocketBinder : Binder() {
        val service: WsClientService
            get() = this@WsClientService
    }

    fun disconnectServer(reason: String) {
        mWebSocket!!.close(DISCONNECT_BY_CLIENT, reason)
        mWebSocketClient = null
        mAllowAutoConnect = false
    }

    fun connectServer(address: String) {
        if(mWebSocketClient != null) {
            if(mIsServerConnected) {
                mWebSocket!!.close(CLOSE_PREVIOUS_CONNECTION, "")
                mWebSocketClient = null
            }
        }

        mAllowAutoConnect = true
        urlServer = "ws://$address:38301"
        doConnectWebSocket()

//        Handler().postDelayed({ timerPingPong(PING_PONG_PERIOD, PING_PONG_INTERVAL) }, PING_PONG_INTERVAL * 2)
    }

    private fun doConnectWebSocket() {
        val listener = EchoWebSocketListener()
        val request = Request.Builder()
                .url(urlServer)
//                .header("system token", sysToken)
                .build()

        mWebSocketClient = OkHttpClient()
        mWebSocket = mWebSocketClient!!.newWebSocket(request, listener)
//        mWebSocketClient!!.dispatcher().executorService().shutdown()
    }


    fun pingServer() {
        if(mIsServerConnected) {
            val jsonObject = JSONObject()
            jsonObject.put(JSON_CMD_TYPE, CMD_KEEP_CONNECTION)
            jsonObject.put(JSON_BODY, CONTENT_KEEP_CONNECTION)

            mWebSocket!!.send(jsonObject.toString())
        } else {
            if(mAllowAutoConnect)   doConnectWebSocket()
        }

    }

    fun replyServer(cmd: String, body: String ) {
        if(mIsServerConnected) {
            val jsonObject = JSONObject()
            jsonObject.put(JSON_CMD_TYPE, cmd)
            jsonObject.put(JSON_BODY, body)

            mWebSocket!!.send(jsonObject.toString())
        } else {
            doConnectWebSocket()
        }
    }

    fun writeToServer(msg: String) {
        if(mIsServerConnected) {
            mWebSocket!!.send(msg)
        }
    }

    fun writePacketToServer(packet: CommPacket) {
        if(mIsServerConnected) {
            val jsonObject = JSONObject()
            jsonObject.put(JSON_CMD_TYPE, CMD_PACKET)
            jsonObject.put(JSON_BODY, packetToString(packet))

            mWebSocket!!.send(jsonObject.toString())
        }
    }

    fun writeJSonPacketToServer(sPacket: String) {
        if(mIsServerConnected) {
            mWebSocket!!.send(sPacket)
        }
    }


    fun setServerURL(url: String) {
        urlServer = url
    }

    companion object {
        const val LOG_TAG = "WsClientService"
        const val ACTION_WEBSOCKET_CLIENT_CONNECTED = "com.digilock.nl.controller.websocketservice.CLIENT_CONNECTED"
        const val ACTION_WEBSOCKET_CLIENT_DISCONNECTED = "com.digilock.nl.controller.websocketservice.CLIENT_DISCONNECTED"
        const val ACTION_WEBSOCKET_CLIENT_DATA_RECEIVED = "com.digilock.nl.controller.websocketservice.CLIENT_DATA_RECEIVED"
        const val ACTION_WEBSOCKET_CLIENT_FAILURE = "com.digilock.nl.controller.websocketservice.CLIENT_FAILURE"
        const val MESSAGE_FROM_WEBSOCKET = 0

        val DISCONNECT_BY_CLIENT = 1000
        val DISCONNECT_BY_SERVER = 1001
        val CLOSE_PREVIOUS_CONNECTION = 1002



        var CLIENT_SERVICE_CONNECTED = false
    }

    inner class EchoWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response?) {
            mIsServerConnected = true

            val intent = Intent(ACTION_WEBSOCKET_CLIENT_CONNECTED)
            sendBroadcast(intent)

            pingServer()
        }

        override fun onMessage(webSocket: WebSocket?, message: String) {
            val jsonObject: JsonObject = JsonParser().parse(message).getAsJsonObject()
            when(jsonObject.get(JSON_CMD_TYPE).asString) {
                CMD_KEEP_CONNECTION -> {
                    Log.e(LOG_TAG, "Server send Ping-Pong response")

                    val intent = Intent(ACTION_WEBSOCKET_CLIENT_DATA_RECEIVED)
                    intent.putExtra(JSON_CMD_TYPE, CMD_KEEP_CONNECTION)
                    intent.putExtra(JSON_BODY, CONTENT_KEEP_CONNECTION)
                    sendBroadcast(intent)
                }
                CMD_PROGRAM_CONTROLLER -> {
                    val result = jsonObject.get(CMD_RESULT).asString
                    val msg = jsonObject.get(JSON_BODY).asString
                    Log.e(LOG_TAG, "Receive Program Controller response: ${result}${msg}")
                    val intent = Intent(ACTION_WEBSOCKET_CLIENT_DATA_RECEIVED)
                    intent.putExtra(JSON_CMD_TYPE, CMD_PROGRAM_CONTROLLER)
                    intent.putExtra(CMD_RESULT, result)
                    intent.putExtra(JSON_BODY, msg)
                    sendBroadcast(intent)
                }
                CMD_LOCK_CRED_ASSIGNMENT -> {
                    Log.e(LOG_TAG, "Server send lock-credential-assignment update")

                    val lockUUID = jsonObject.get(LOCK_UUID).asString
                    val lockUID = jsonObject.get(LOCK_UID).asString
                    val lockFunc = jsonObject.get(LOCK_FUNC).asString

                    val credUUID = jsonObject.get(CREDENTIAL_UUID).asString
                    val credType = jsonObject.get(CREDENTIAL_TYPE).asString
                    val credValue = jsonObject.get(CREDENTIAL_VALUE).asString

                    val intent = Intent(ACTION_WEBSOCKET_CLIENT_DATA_RECEIVED)
                    intent.putExtra(JSON_CMD_TYPE, CMD_LOCK_CRED_ASSIGNMENT)
                    intent.putExtra(LOCK_UUID, lockUUID)
                    intent.putExtra(LOCK_UID, lockUID)
                    intent.putExtra(LOCK_FUNC, lockFunc)
                    intent.putExtra(CREDENTIAL_UUID, credUUID)
                    intent.putExtra(CREDENTIAL_TYPE, credType)
                    intent.putExtra(CREDENTIAL_VALUE, credValue)
                    sendBroadcast(intent)
                }

                CMD_LOCK_CRED_DEASSIGNMENT -> {
                    Log.e(LOG_TAG, "Server send lock-credential-deassignment")

                    val lockUUID = jsonObject.get(LOCK_UUID).asString
                    val credUUID = jsonObject.get(CREDENTIAL_UUID).asString

                    val intent = Intent(ACTION_WEBSOCKET_CLIENT_DATA_RECEIVED)
                    intent.putExtra(JSON_CMD_TYPE, CMD_LOCK_CRED_DEASSIGNMENT)
                    intent.putExtra(LOCK_UUID, lockUUID)
                    intent.putExtra(CREDENTIAL_UUID, credUUID)
                    sendBroadcast(intent)
                }
            }

        }

        override fun onMessage(webSocket: WebSocket?, bytes: ByteString) {

        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(DISCONNECT_BY_SERVER, "")

            mIsServerConnected = false

            val intent = Intent(ACTION_WEBSOCKET_CLIENT_DISCONNECTED)
            sendBroadcast(intent)
        }

        override fun onFailure(webSocket: WebSocket?, t: Throwable, response: Response?) {
            val intent = Intent(ACTION_WEBSOCKET_CLIENT_FAILURE)
            sendBroadcast(intent)
        }
    }

}