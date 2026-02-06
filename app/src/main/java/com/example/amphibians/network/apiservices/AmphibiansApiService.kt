package com.example.amphibians.network.apiservices

import com.example.amphibians.model.amphibians.Amphibian
import retrofit2.http.GET
import retrofit2.http.Query

interface AmphibiansApiService {
    @GET("amphibians")
    suspend fun getAmphibians(): List<Amphibian>


    @GET("amphibians")
    suspend fun searchAmphibians(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10
    ): List<Amphibian>
}
