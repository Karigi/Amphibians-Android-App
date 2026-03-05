package com.example.amphibians.network.apiservices

import com.example.amphibians.model.amphibians.GbifOccurrence
import com.example.amphibians.model.amphibians.GbifPagedResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GbifApiService {

    /**
     * Search occurrence records (observations/specimens).
     *
     * Example URL produced:
     *  /occurrence/search?taxonKey=131&mediaType=StillImage&hasCoordinate=true&limit=50&offset=0
     *
     * taxonKey=131 means "Amphibia class" (all amphibians under it).
     * Paging uses limit + offset. (GBIF standard)
     */
    @GET("occurrence/search")
    suspend fun searchOccurrences(
        @Query("taxonKey") taxonKey: Int,                 // 131 = Amphibia
        @Query("mediaType") mediaType: String? = null,    // StillImage / Sound / MovingImage
        @Query("hasCoordinate") hasCoordinate: Boolean? = null, // true = only records with lat/lng
        @Query("country") country: String? = null,        // "KE", "US", etc (ISO-2)
        @Query("limit") limit: Int = 50,                  // page size
        @Query("offset") offset: Int = 0                  // page start index
    ): GbifPagedResponse<GbifOccurrence>

    /**
     * Fetch a single occurrence by its GBIF key.
     * Useful for a details screen if you want more fields than the list returns.
     */
    @GET("occurrence/{key}")
    suspend fun getOccurrence(
        @Path("key") key: Long
    ): GbifOccurrence
}