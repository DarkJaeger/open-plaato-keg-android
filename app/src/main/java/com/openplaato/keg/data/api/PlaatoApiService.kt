package com.openplaato.keg.data.api

import com.openplaato.keg.data.model.AirlockEnabledBody
import com.openplaato.keg.data.model.TransferScale
import com.openplaato.keg.data.model.TransferScaleConfigBody
import com.openplaato.keg.data.model.AppConfigResponse
import com.openplaato.keg.data.model.BrewfatherBatch
import com.openplaato.keg.data.model.BrewfatherBody
import com.openplaato.keg.data.model.BrewfatherConfigResponse
import com.openplaato.keg.data.model.BrewfatherCredsBody
import com.openplaato.keg.data.model.GrainfatherBody
import com.openplaato.keg.data.model.Airlock
import com.openplaato.keg.data.model.Beverage
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.data.model.StatusResponse
import com.openplaato.keg.data.model.Tap
import com.openplaato.keg.data.model.TapHandleUploadResponse
import com.openplaato.keg.data.model.TapSaveBody
import com.openplaato.keg.data.model.ValueBody
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PlaatoApiService {

    // Taps (beer.db)
    @GET("api/taps")
    suspend fun getTaps(): List<Tap>

    @GET("api/taps/{id}")
    suspend fun getTap(@Path("id") id: String): Tap

    @POST("api/taps/{id}")
    suspend fun saveTap(@Path("id") id: String, @Body body: TapSaveBody): StatusResponse

    @POST("api/taps/{id}/delete")
    suspend fun deleteTap(@Path("id") id: String): StatusResponse

    // Tap handles
    @GET("api/tap-handles")
    suspend fun getTapHandles(): List<String>

    @Multipart
    @POST("api/tap-handles/upload")
    suspend fun uploadTapHandle(@Part file: MultipartBody.Part): TapHandleUploadResponse

    // Kegs (live data)
    @GET("api/kegs")
    suspend fun getKegs(): List<Keg>

    // Scale commands
    @POST("api/kegs/{id}/unit")
    suspend fun setUnit(@Path("id") id: String, @Body body: ValueBody): StatusResponse

    @POST("api/kegs/{id}/measure-unit")
    suspend fun setMeasureUnit(@Path("id") id: String, @Body body: ValueBody): StatusResponse

    @POST("api/kegs/{id}/sensitivity")
    suspend fun setSensitivity(@Path("id") id: String, @Body body: ValueBody): StatusResponse

    @POST("api/kegs/{id}/keg-mode")
    suspend fun setKegMode(@Path("id") id: String, @Body body: ValueBody): StatusResponse

    @POST("api/kegs/{id}/tare")
    suspend fun tare(@Path("id") id: String): StatusResponse

    @POST("api/kegs/{id}/tare-release")
    suspend fun tareRelease(@Path("id") id: String): StatusResponse

    @POST("api/kegs/{id}/empty-keg")
    suspend fun emptyKeg(@Path("id") id: String): StatusResponse

    @POST("api/kegs/{id}/empty-keg-release")
    suspend fun emptyKegRelease(@Path("id") id: String): StatusResponse

    @POST("api/kegs/{id}/empty-keg-weight")
    suspend fun setEmptyKegWeight(@Path("id") id: String, @Body body: ValueBody): StatusResponse

    @POST("api/kegs/{id}/max-keg-volume")
    suspend fun setMaxKegVolume(@Path("id") id: String, @Body body: ValueBody): StatusResponse

    @POST("api/kegs/{id}/calibrate-known-weight")
    suspend fun calibrateKnownWeight(@Path("id") id: String, @Body body: ValueBody): StatusResponse

    @POST("api/kegs/{id}/temperature-offset")
    suspend fun setTemperatureOffset(@Path("id") id: String, @Body body: ValueBody): StatusResponse

    @POST("api/kegs/{id}/reset-last-pour")
    suspend fun resetLastPour(@Path("id") id: String): StatusResponse

    // App config
    @GET("api/config")
    suspend fun getAppConfig(): AppConfigResponse

    @POST("api/config/airlock-enabled")
    suspend fun setAirlockEnabled(@Body body: AirlockEnabledBody): StatusResponse

    @GET("api/config/brewfather")
    suspend fun getBrewfatherConfig(): BrewfatherConfigResponse

    @POST("api/config/brewfather")
    suspend fun saveBrewfatherCreds(@Body body: BrewfatherCredsBody): StatusResponse

    // Brewfather batch import
    @GET("api/brewfather/batches")
    suspend fun getBrewfatherBatches(): List<BrewfatherBatch>

    @POST("api/brewfather/import/{batchId}")
    suspend fun importBrewfatherBatch(@Path("batchId") batchId: String): StatusResponse

    // Airlocks
    @GET("api/airlocks")
    suspend fun getAirlocks(): List<Airlock>

    @POST("api/airlocks/{id}/label")
    suspend fun setAirlockLabel(@Path("id") id: String, @Body body: ValueBody): StatusResponse

    @POST("api/airlocks/{id}/grainfather")
    suspend fun setGrainfather(@Path("id") id: String, @Body body: GrainfatherBody): StatusResponse

    @POST("api/airlocks/{id}/brewfather")
    suspend fun setBrewfather(@Path("id") id: String, @Body body: BrewfatherBody): StatusResponse

    // Beverages
    @GET("api/beverages")
    suspend fun getBeverages(): List<Beverage>

    @POST("api/beverages/{id}")
    suspend fun saveBeverage(@Path("id") id: String, @Body body: Beverage): StatusResponse

    @POST("api/beverages/{id}/delete")
    suspend fun deleteBeverage(@Path("id") id: String): StatusResponse

    // Transfer scales
    @GET("api/transfer-scales")
    suspend fun getTransferScales(): List<TransferScale>

    @GET("api/transfer-scales/{id}")
    suspend fun getTransferScale(@Path("id") id: String): TransferScale

    @POST("api/transfer-scales/{id}/config")
    suspend fun configureTransferScale(@Path("id") id: String, @Body body: TransferScaleConfigBody): StatusResponse

    @POST("api/transfer-scales/{id}/delete")
    suspend fun deleteTransferScale(@Path("id") id: String): StatusResponse
}
