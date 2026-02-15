package io.bluetape4k.aws.kotlin.s3.model

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import java.io.Serializable

data class Location(
    val bucket: String,
    val key: String,
    val version: String? = null,
): Serializable, Comparable<Location> {

    companion object: KLogging() {
        const val S3_PROTOCOL_SCHEME = "s3://"
        const val PATH_DELIMITER = "/"
        const val VERSION_DELIMITER = "^"

        @JvmStatic
        operator fun invoke(locationUrl: String): Location {
            val (bucket, key, version) = resolve(locationUrl)
            bucket.requireNotBlank("bucket")
            key.requireNotBlank("key")

            return Location(bucket, key, version)
        }

        fun isS3Resource(locationUrl: String): Boolean {
            return locationUrl.startsWith(S3_PROTOCOL_SCHEME)
        }

        private fun resolve(locationUrl: String): Triple<String, String, String?> {
            var uri = locationUrl.removePrefix(S3_PROTOCOL_SCHEME)
            log.trace { "uri=$uri" }
            val bucketEndIndex = uri.indexOf(PATH_DELIMITER)

            bucketEndIndex.requirePositiveNumber("bucketEndIndex")
            val bucket = uri.substring(0, bucketEndIndex)

            uri = uri.substring(bucketEndIndex + 1)
            log.trace { "uri=$uri" }

            val keyEndIndex = if (uri.contains(VERSION_DELIMITER)) {
                uri.indexOf(VERSION_DELIMITER)
            } else {
                uri.length
            }
            val key = uri.substring(0, keyEndIndex)

            val version = if (uri.length > keyEndIndex) {
                uri.substring(keyEndIndex + 1)
            } else {
                null
            }

            return Triple(bucket, key, version)
        }
    }

    val url: String
        get() = "$S3_PROTOCOL_SCHEME$bucket$PATH_DELIMITER$key${version?.let { "$VERSION_DELIMITER$it" }.orEmpty()}"

    override fun compareTo(other: Location): Int {
        return compareValuesBy(this, other, Location::bucket, Location::key, Location::version)
    }
}
