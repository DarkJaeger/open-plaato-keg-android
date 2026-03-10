package com.openplaato.keg.data.api

import com.openplaato.keg.data.model.Airlock
import com.openplaato.keg.data.model.Beverage
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.data.model.StatusResponse
import com.openplaato.keg.data.model.Tap
import com.openplaato.keg.data.model.TapSaveBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    // Kegs (live data)
    @GET("api/kegs")
    suspend fun getKegs(): List<Keg>

    @POST("api/kegs/{id}/tare")
    suspend fun tare(@Path("id") id: String): StatusResponse

    @POST("api/kegs/{id}/tare-release")
    suspend fun tareRelease(@Path("id") id: String): StatusResponse

    // Airlocks
    @GET("api/airlocks")
    suspend fun getAirlocks(): List<Airlock>

    // Beverages
    @GET("api/beverages")
    suspend fun getBeverages(): List<Beverage>

    @POST("api/beverages/{id}")
    suspend fun saveBeverage(@Path("id") id: String, @Body body: Beverage): StatusResponse

    @POST("api/beverages/{id}/delete")
    suspend fun deleteBeverage(@Path("id") id: String): StatusResponse
}
