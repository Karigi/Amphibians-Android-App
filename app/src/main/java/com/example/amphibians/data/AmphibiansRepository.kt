package com.example.amphibians.data


import com.example.amphibians.model.amphibians.Amphibian
import com.example.amphibians.model.amphibians.AmphibiansPage
import com.example.amphibians.model.amphibians.GbifOccurrence
import com.example.amphibians.model.amphibians.gbifCachedImageUrl
import com.example.amphibians.network.apiservices.GbifApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
     *
     * Returns a cold Flow that emits ONE page, then completes
     */
    fun getAmphibiansPageFlow(offset: Int, limit: Int): Flow<AmphibiansPage>
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
     * This function does NOT immediately perform the network call.
     *
     * It RETURNS a Flow object.
     *
     * The actual code inside flow { ... } runs only when somebody collects it.
     *
     * That is what "cold flow" means:
     * - create flow now
     * - execute later when collected
     */
    override fun getAmphibiansPageFlow(offset: Int, limit: Int): Flow<AmphibiansPage> = flow {

        /**
         * Everything inside this block is the "producer" side.
         * This is where values are created and emitted.
         */

        // 1) Call GBIF occurrence search with paging
        val response = gbifApi.searchOccurrences(
            taxonKey = AMPHIBIA_TAXON_KEY, // Amphibia
            mediaType = MEDIA_TYPE_IMAGE,  // only occurrences with images
            hasCoordinate = true,          // only with lat/lng (optional but useful)
            limit = limit,
            offset = offset
        )

        /**
         * 2) Convert the raw network models (GbifOccurrence)
         *    into your app UI model (Amphibian).
         *
         * mapNotNull means:
         * - transform each item
         * - if transformation returns null, skip that item
         *
         * We skip occurrences with no usable image URL.
         */
        val amphibians = response.results.mapNotNull { occ ->
            // Find the first media item with a non-null identifier (URL)
            val mediaUrl = occ.media?.firstOrNull { it.identifier != null }?.identifier?: return@mapNotNull null

            // Build a GBIF cached image URL
            val imgUrl = gbifCachedImageUrl(
                occurrenceKey = occ.key,
                mediaIdentifierUrl = mediaUrl,
                sizePrefix = "400x"
            )

            // Build a description string for the card/details screen
            val description = buildString {
                append("Country: ${occ.country ?: "Unknown"}")
                if (!occ.eventDate.isNullOrBlank()) append("\nDate: ${occ.eventDate}")
                if (!occ.species.isNullOrBlank()) append("\nSpecies: ${occ.species}")
                if (!occ.scientificName.isNullOrBlank()) append("\nScientific name: ${occ.scientificName}")
            }

            // Return one Amphibian item
            Amphibian(
                id = occ.key,
                name = occ.species ?: occ.scientificName ?: "Unknown",
                type = "Occurrence",
                description = description,
                imgSrc = imgUrl
            )

        }

        /**
         * 3) Emit ONE value downstream.
         *
         * That one emitted value is an AmphibiansPage object.
         *
         * Important:
         * We are emitting ONE-page object.
         * That page object itself contains a LIST of Amphibian items.
         */
        emit(
            AmphibiansPage(
                items = amphibians,
                endOfRecords = response.endOfRecords
            )
        )

    }

        /**
         * flowOn moves the UPSTREAM work to IO.
         *
         * "Upstream" means the work above this line:
         * - the API call
         * - the mapping
         * - the emit
         *
         * Without this, that upstream work would run in the collector's context.
         */
        .flowOn(Dispatchers.IO)

}