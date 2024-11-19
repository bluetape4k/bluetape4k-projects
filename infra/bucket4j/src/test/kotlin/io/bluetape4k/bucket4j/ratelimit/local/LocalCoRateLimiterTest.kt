package io.bluetape4k.bucket4j.ratelimit.local

import io.bluetape4k.bucket4j.local.LocalCoBucketProvider
import io.bluetape4k.bucket4j.ratelimit.AbstractCoRateLimiterTest
import io.bluetape4k.bucket4j.ratelimit.CoRateLimiter
import io.bluetape4k.logging.KLogging

class LocalCoRateLimiterTest: AbstractCoRateLimiterTest() {

    companion object: KLogging()

    val bucketProvider by lazy {
        LocalCoBucketProvider(defaultBucketConfiguration)
    }

    override val rateLimiter: CoRateLimiter<String> by lazy {
        LocalCoRateLimiter(bucketProvider)
    }
}
