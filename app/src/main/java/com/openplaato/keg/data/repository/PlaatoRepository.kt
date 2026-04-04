package com.openplaato.keg.data.repository

import android.content.Context
import android.net.Uri
import com.openplaato.keg.data.api.PlaatoApiService
import com.openplaato.keg.data.api.WebSocketManager
import com.openplaato.keg.data.api.WsEvent
import com.openplaato.keg.data.model.AirlockEnabledBody
import com.openplaato.keg.data.model.TransferScale
import com.openplaato.keg.data.model.TransferScaleConfigBody
import com.openplaato.keg.data.model.Airlock
import com.openplaato.keg.data.model.BrewfatherBody
import com.openplaato.keg.data.model.BrewfatherCredsBody
import com.openplaato.keg.data.model.GrainfatherBody
import com.openplaato.keg.data.model.Beverage
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.data.model.StatusResponse
import com.openplaato.keg.data.model.Tap
import com.openplaato.keg.data.model.TapHandleUploadResponse
import com.openplaato.keg.data.model.ValueBody
import com.openplaato.keg.data.model.TapSaveBody
import com.openplaato.keg.data.preferences.AppPreferences
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    suspend fun getTapHandles(): Result<List<String>> = runCatching { api.getTapHandles() }

    suspend fun uploadTapHandle(context: Context, uri: Uri): Result<TapHandleUploadResponse> = runCatching {
        val stream = context.contentResolver.openInputStream(uri) ?: error("Cannot open image")
        val bytes = stream.use { it.readBytes() }
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mimeType) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val part = MultipartBody.Part.createFormData(
            "file", "handle.$ext", bytes.toRequestBody(mimeType.toMediaType())
        )
        api.uploadTapHandle(part)
    }

    suspend fun getKegs(): Result<List<Keg>> = runCatching { api.getKegs() }

    // Scale commands
    suspend fun setUnit(id: String, value: String) = runCatching { api.setUnit(id, ValueBody(value)) }
    suspend fun setMeasureUnit(id: String, value: String) = runCatching { api.setMeasureUnit(id, ValueBody(value)) }
    suspend fun setSensitivity(id: String, value: String) = runCatching { api.setSensitivity(id, ValueBody(value)) }
    suspend fun setKegMode(id: String, value: String) = runCatching { api.setKegMode(id, ValueBody(value)) }
    suspend fun tare(kegId: String) = runCatching { api.tare(kegId) }
    suspend fun tareRelease(kegId: String) = runCatching { api.tareRelease(kegId) }
    suspend fun emptyKeg(kegId: String) = runCatching { api.emptyKeg(kegId) }
    suspend fun emptyKegRelease(kegId: String) = runCatching { api.emptyKegRelease(kegId) }
    suspend fun setEmptyKegWeight(id: String, value: String) = runCatching { api.setEmptyKegWeight(id, ValueBody(value)) }
    suspend fun setMaxKegVolume(id: String, value: String) = runCatching { api.setMaxKegVolume(id, ValueBody(value)) }
    suspend fun calibrateKnownWeight(id: String, value: String) = runCatching { api.calibrateKnownWeight(id, ValueBody(value)) }
    suspend fun setTemperatureOffset(id: String, value: String) = runCatching { api.setTemperatureOffset(id, ValueBody(value)) }
    suspend fun resetLastPour(kegId: String) = runCatching { api.resetLastPour(kegId) }

    // App config
    suspend fun getAppConfig() = runCatching { api.getAppConfig() }
    suspend fun setAirlockEnabled(enabled: Boolean) = runCatching { api.setAirlockEnabled(AirlockEnabledBody(enabled)) }
    suspend fun getBrewfatherConfig() = runCatching { api.getBrewfatherConfig() }
    suspend fun saveBrewfatherCreds(userId: String, apiKey: String) = runCatching { api.saveBrewfatherCreds(BrewfatherCredsBody(userId, apiKey)) }

    // Brewfather batch import
    suspend fun getBrewfatherBatches() = runCatching { api.getBrewfatherBatches() }
    suspend fun importBrewfatherBatch(batchId: String) = runCatching { api.importBrewfatherBatch(batchId) }

    // Airlocks
    suspend fun getAirlocks(): Result<List<Airlock>> = runCatching { api.getAirlocks() }
    suspend fun setAirlockLabel(id: String, label: String) = runCatching { api.setAirlockLabel(id, ValueBody(label)) }
    suspend fun setGrainfather(id: String, body: GrainfatherBody) = runCatching { api.setGrainfather(id, body) }
    suspend fun setBrewfather(id: String, body: BrewfatherBody) = runCatching { api.setBrewfather(id, body) }

    suspend fun getBeverages(): Result<List<Beverage>> = runCatching { api.getBeverages() }
    suspend fun saveBeverage(id: String, body: Beverage): Result<StatusResponse> = runCatching { api.saveBeverage(id, body) }
    suspend fun deleteBeverage(id: String): Result<StatusResponse> = runCatching { api.deleteBeverage(id) }

    // Transfer scales
    suspend fun getTransferScales(): Result<List<TransferScale>> = runCatching { api.getTransferScales() }
    suspend fun getTransferScale(id: String): Result<TransferScale> = runCatching { api.getTransferScale(id) }
    suspend fun configureTransferScale(id: String, body: TransferScaleConfigBody): Result<StatusResponse> = runCatching { api.configureTransferScale(id, body) }
    suspend fun deleteTransferScale(id: String): Result<StatusResponse> = runCatching { api.deleteTransferScale(id) }
}
