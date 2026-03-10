package com.openplaato.keg.data.repository

import com.openplaato.keg.data.api.PlaatoApiService
import com.openplaato.keg.data.api.WebSocketManager
import com.openplaato.keg.data.api.WsEvent
import com.openplaato.keg.data.model.Airlock
import com.openplaato.keg.data.model.Beverage
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.data.model.StatusResponse
import com.openplaato.keg.data.model.Tap
import com.openplaato.keg.data.model.TapSaveBody
import com.openplaato.keg.data.preferences.AppPreferences
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaatoRepository @Inject constructor(
    private val api: PlaatoApiService,
    private val wsManager: WebSocketManager,
    private val prefs: AppPreferences,
) {
    val wsEvents: SharedFlow<WsEvent> get() = wsManager.events
    val serverUrl get() = prefs.serverUrl

    fun connectWebSocket(baseUrl: String) = wsManager.connect(baseUrl)
    fun disconnectWebSocket() = wsManager.disconnect()

    suspend fun saveServerUrl(url: String) = prefs.setServerUrl(url)

    suspend fun getTaps(): Result<List<Tap>> = runCatching { api.getTaps() }
    suspend fun saveTap(id: String, body: TapSaveBody): Result<StatusResponse> = runCatching { api.saveTap(id, body) }
    suspend fun deleteTap(id: String): Result<StatusResponse> = runCatching { api.deleteTap(id) }

    suspend fun getKegs(): Result<List<Keg>> = runCatching { api.getKegs() }
    suspend fun tare(kegId: String): Result<StatusResponse> = runCatching { api.tare(kegId) }
    suspend fun tareRelease(kegId: String): Result<StatusResponse> = runCatching { api.tareRelease(kegId) }

    suspend fun getAirlocks(): Result<List<Airlock>> = runCatching { api.getAirlocks() }

    suspend fun getBeverages(): Result<List<Beverage>> = runCatching { api.getBeverages() }
    suspend fun saveBeverage(id: String, body: Beverage): Result<StatusResponse> = runCatching { api.saveBeverage(id, body) }
    suspend fun deleteBeverage(id: String): Result<StatusResponse> = runCatching { api.deleteBeverage(id) }
}
