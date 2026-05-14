package io.bluetape4k.bucket4j

import io.bluetape4k.support.toUtf8Bytes

/**
 * Maximum serialized bucket-key size accepted by bluetape4k Bucket4j providers.
 *
 * The cap is applied after the provider prefix is appended. It prevents
 * unbounded user-provided keys from amplifying local cache or Redis memory use.
 */
const val MAX_BUCKET_KEY_BYTES: Int = 512

internal fun validateBucketKeySize(bucketKey: String, name: String = "bucketKey"): String {
    validateBucketKeySize(bucketKey.toUtf8Bytes(), name)
    return bucketKey
}

internal fun validateBucketKeySize(bucketKey: ByteArray, name: String = "bucketKey"): ByteArray {
    require(bucketKey.size <= MAX_BUCKET_KEY_BYTES) {
        "$name must be at most $MAX_BUCKET_KEY_BYTES bytes after prefix encoding. actual=${bucketKey.size}"
    }
    return bucketKey
}
