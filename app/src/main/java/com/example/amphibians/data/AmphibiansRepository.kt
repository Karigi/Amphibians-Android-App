package com.example.amphibians.data

import com.example.amphibians.model.amphibians.Amphibian
import com.example.amphibians.network.apiservices.AmphibiansApiService
import javax.inject.Inject

interface AmphibiansRepository{
    suspend fun getAmphibians(): List<Amphibian>
    suspend fun searchAmphibians(
        query: String,
        limit: Int = 10
    ): List<Amphibian>
}

class NetworkAmphibiansRepository @Inject constructor(
    private val amphibiansApiService: AmphibiansApiService
): AmphibiansRepository{
    override suspend fun getAmphibians(): List<Amphibian> {
        return amphibiansApiService.getAmphibians()
    }

    override suspend fun searchAmphibians(query: String, limit: Int): List<Amphibian> {
        return amphibiansApiService.searchAmphibians(query = query, limit = limit)
    }
}