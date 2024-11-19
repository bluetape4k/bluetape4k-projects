package io.bluetape4k.bucket4j.ratelimit.local

import io.bluetape4k.bucket4j.local.LocalBucketProvider
import io.bluetape4k.bucket4j.ratelimit.AbstractRateLimiterTest
import io.bluetape4k.bucket4j.ratelimit.RateLimiter
import io.bluetape4k.logging.KLogging

class LocalRateLimiterTest: AbstractRateLimiterTest() {

    companion object: KLogging()

    val bucketProvider by lazy {
        LocalBucketProvider(defaultBucketConfiguration)
    }

    override val rateLimiter: RateLimiter<String> by lazy {
        LocalRateLimiter(bucketProvider)
    }

}
