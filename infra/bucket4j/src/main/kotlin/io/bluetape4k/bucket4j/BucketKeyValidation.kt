package io.bluetape4k.bucket4j

import io.bluetape4k.support.toUtf8Bytes

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
