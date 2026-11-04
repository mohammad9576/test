package com.kodomo.app.data.remote

import android.util.Log
import com.kodomo.app.data.prefs.SessionPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streams live usage heartbeats to the backend WebSocket gateway so that a parent's phone or the admin
 * dashboard can monitor consumption in real time. Silently stays offline when no cloud account is linked.
 */
@Singleton
class LiveUsageSocket @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val session: SessionPreferences,
    private val apiProvider: ApiProvider,
) {
    enum class Status { OFFLINE, CONNECTING, CONNECTED }

    private val _status = MutableStateFlow(Status.OFFLINE)
    val status: StateFlow<Status> = _status.asStateFlow()

    @Volatile private var socket: WebSocket? = null

    suspend fun ensureConnected() {
        if (socket != null) return
        val token = session.current().authToken ?: return
        _status.value = Status.CONNECTING
        val request = Request.Builder()
            .url(apiProvider.webSocketUrl())
            .header("Authorization", "Bearer $token")
            .build()
        val wsClient = client.newBuilder().pingInterval(25, TimeUnit.SECONDS).build()
        socket = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.value = Status.CONNECTED
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                _status.value = Status.OFFLINE
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Live usage socket failed: ${t.message}")
                socket = null
                _status.value = Status.OFFLINE
            }
        })
    }

    fun send(event: UsageEventDto) {
        socket?.send(json.encodeToString(UsageEventDto.serializer(), event))
    }

    fun close() {
        socket?.close(1000, "client closing")
        socket = null
        _status.value = Status.OFFLINE
    }

    private companion object { const val TAG = "LiveUsageSocket" }
}
