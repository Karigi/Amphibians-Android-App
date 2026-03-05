package com.example.amphibians.data


import com.example.amphibians.model.amphibians.Amphibian
import com.example.amphibians.model.amphibians.AmphibiansPage
import com.example.amphibians.model.amphibians.GbifOccurrence
import com.example.amphibians.model.amphibians.gbifCachedImageUrl
import com.example.amphibians.network.apiservices.GbifApiService
import javax.inject.Inject


private const val AMPHIBIA_TAXON_KEY = 131
private const val MEDIA_TYPE_IMAGE = "stillImage"


/**
 * Repository is the "data provider" for the UI.
 *
 * ViewModel talks to repository.
 * Repository talks to the network (Retrofit) and converts network models
 * into UI models.
 */
interface AmphibiansRepository{
    /**
     * Loads one page using offset + limit.
     */
    suspend fun getAmphibiansPage(offset: Int, limit: Int): AmphibiansPage
}

/**
 * Network implementation of AmphibiansRepository.
 *
 * It uses GBIF Occurrence Search API to get occurrences with images,
 * then maps them into your app's Amphibian model.
 */
class NetworkAmphibiansRepository @Inject constructor (
    private val gbifApi: GbifApiService
) : AmphibiansRepository {

    /**
     * Loads amphibians (occurrence records) that have images.
     * This is used by the Home screen.
     */
    override suspend fun getAmphibiansPage(offset: Int, limit: Int): AmphibiansPage {

        // 1) Call GBIF occurrence search
        val page = gbifApi.searchOccurrences(
            taxonKey = AMPHIBIA_TAXON_KEY, // Amphibia
            mediaType = MEDIA_TYPE_IMAGE,  // only occurrences with images
            hasCoordinate = true,          // only with lat/lng (optional but useful)
            limit = limit,
            offset = offset
        )

        // 2) Map occurrences -> Amphibian, skipping ones that don't have an image url
        val mappedItems: List<Amphibian> = page.results
            .mapNotNull { it.toAmphibianOrNull(imageSize = "400x") }

        // 3) Return a small wrapper that includes endOfRecords
        return AmphibiansPage(
            items = mappedItems,
            endOfRecords = page.endOfRecords
        )
    }

    /**
     * Converts a GBIF occurrence into Amphibian.
     * Returns null if we can't find a media identifier URL.
     */
    private fun GbifOccurrence.toAmphibianOrNull(imageSize: String): Amphibian? {

        // A) Pick the first media item that has a URL
        val mediaUrl: String = media
            ?.firstOrNull { it.identifier != null }
            ?.identifier
            ?: return null // If no image URL exists, skip this occurrence

        // B) Build a GBIF cached image URL (faster and can be resized)
        val cachedImageUrl: String = gbifCachedImageUrl(
            occurrenceKey = key,
            mediaIdentifierUrl = mediaUrl,
            sizePrefix = imageSize
        )

        // C) Build a nice description string
        val desc: String = buildString {
            append("Country: ${country ?: "Unknown"}")
            if (!eventDate.isNullOrBlank()) append("\nDate: $eventDate")
        }

        // D) Return your UI model
        return Amphibian(
            id = key,
            name = species ?: scientificName ?: "Unknown",
            type = "Occurrence",
            description = desc,
            imgSrc = cachedImageUrl
        )
    }

}