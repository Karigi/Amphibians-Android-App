package com.example.amphibians.model.amphibians

import kotlinx.serialization.Serializable
import java.security.MessageDigest

/**
 * GBIF endpoints that return a "search result" usually use paging:
 *
 * - limit  = how many items you asked for
 * - offset = where the page starts (0, 50, 100...)
 *
 * GBIF returns a wrapper object with metadata + results[].
 * This class matches that wrapper.
 */
@Serializable
data class GbifPagedResponse<T>(
    val offset: Int = 0,                // The offset you requested for this page
    val limit: Int = 0,                 // The page size you requested
    val endOfRecords: Boolean = true,   // True means "no more pages after this"
    val count: Long = 0,                // Total matching records across all pages
    val results: List<T> = emptyList()  // The page items themselves
)

/**
 * A GBIF "Occurrence" is a record of an observation/specimen.
 *
 * IMPORTANT:
 * - This is NOT a "species catalog item".
 * - It's a specific occurrence with location/date/media etc.
 */
@Serializable
data class GbifOccurrence(
    val key: Long,                       // Unique ID for this occurrence (stable ID!)
    val scientificName: String? = null,  // Full scientific name (often includes authorship)
    val species: String? = null,         // Species name (if GBIF parsed it cleanly)
    val country: String? = null,         // Country name (may be null)
    val eventDate: String? = null,       // Date string (can be null)
    /**
     * media[] is where images/sounds/videos are usually listed.
     * For images, the most important field is media.identifier (usually a URL).
     */
    val media: List<GbifMedia>? = null
)

/**
 * One media item attached to an occurrence.
 * identifier is commonly a direct URL to the image.
 */
@Serializable
data class GbifMedia(
    val identifier: String? = null,   // URL to image file (most important for you)
    val type: String? = null,         // e.g. "StillImage"
    val format: String? = null,       // e.g. "image/jpeg"
    val license: String? = null,      // license, useful for attribution
    val rightsHolder: String? = null, // who owns it
    val creator: String? = null,      // photographer / author
    val title: String? = null         // optional caption
)

data class Amphibian(
    val id: Long,
    val name: String,
    val type: String,
    val description: String,
    val imgSrc: String
)

/**
 * A simple wrapper for pagination results from the repository.
 *
 * items        -> the mapped Amphibian items for this page
 * endOfRecords -> true means there are no more pages after this one
 */
data class AmphibiansPage(
    val items: List<Amphibian>,
    val endOfRecords: Boolean
)

/**
 * GBIF cached image API uses md5(identifier) in the URL path.
 *
 * This function:
 * 1) converts the input string to UTF-8 bytes
 * 2) hashes it using MD5
 * 3) converts the byte array to a lowercase hex string
 */
fun md5Hex(input: String): String {
    val md = MessageDigest.getInstance("MD5")                  // MD5 hashing engine
    val bytes = md.digest(input.toByteArray(Charsets.UTF_8))   // hash UTF-8 bytes
    return bytes.joinToString("") { "%02x".format(it) }        // bytes -> hex string
}

/**
 * Builds a GBIF cached image URL from:
 * - occurrenceKey (occurrence.key)
 * - mediaIdentifierUrl (media.identifier)
 *
 * You can optionally request a resized cached image:
 * - "200x" for small thumbnails
 * - "400x" for larger thumbnails
 *
 * If sizePrefix is null, you get the default cached variant.
 */
fun gbifCachedImageUrl(
    occurrenceKey: Long,
    mediaIdentifierUrl: String,
    sizePrefix: String? = "200x"
): String {
    val md5 = md5Hex(mediaIdentifierUrl) // GBIF uses md5(identifier) in the cache URL

    // If you want a resized cached image:
    return if (sizePrefix != null) {
        "https://api.gbif.org/v1/image/cache/$sizePrefix/occurrence/$occurrenceKey/media/$md5"
    } else {
        // Default cached variant:
        "https://api.gbif.org/v1/image/cache/occurrence/$occurrenceKey/media/$md5"
    }
}