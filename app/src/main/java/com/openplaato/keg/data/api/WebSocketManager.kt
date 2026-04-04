package com.openplaato.keg.data.api

import com.openplaato.keg.data.model.Airlock
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.data.model.TransferScale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

sealed class WsEvent {
    data class KegUpdate(val keg: Keg) : WsEvent()
    data class AirlockUpdate(val airlock: Airlock) : WsEvent()
    data class TransferScaleUpdate(val scale: TransferScale) : WsEvent()
}

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events

    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null

    fun connect(baseUrl: String) {
        val wsUrl = baseUrl
            .trimEnd('/')
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/ws"

        if (wsUrl == currentUrl && webSocket != null) return

        disconnect()
        currentUrl = wsUrl

        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@WebSocketManager.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@WebSocketManager.webSocket = null
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, null)
        webSocket = null
        currentUrl = null
    }

    private fun handleMessage(text: String) {
        try {
            val obj = json.parseToJsonElement(text).jsonObject
            val type = obj["type"]?.jsonPrimitive?.content
            if (type == "airlock") {
                val data = obj["data"]?.jsonObject ?: return
                val airlock = json.decodeFromJsonElement<Airlock>(data)
                _events.tryEmit(WsEvent.AirlockUpdate(airlock))
            } else if (type == "transfer_scale") {
                val data = obj["data"]?.jsonObject ?: return
                val scale = json.decodeFromJsonElement<TransferScale>(data)
                _events.tryEmit(WsEvent.TransferScaleUpdate(scale))
            } else {
                val keg = json.decodeFromJsonElement<Keg>(obj)
                _events.tryEmit(WsEvent.KegUpdate(keg))
            }
        } catch (_: Exception) {}
    }
}
