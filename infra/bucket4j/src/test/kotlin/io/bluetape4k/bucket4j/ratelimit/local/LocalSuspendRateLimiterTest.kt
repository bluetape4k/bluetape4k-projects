package io.bluetape4k.bucket4j.ratelimit.local

import io.bluetape4k.bucket4j.local.LocalSuspendBucketProvider
import io.bluetape4k.bucket4j.ratelimit.AbstractSuspendRateLimiterTest
import io.bluetape4k.bucket4j.ratelimit.SuspendRateLimiter
import io.bluetape4k.logging.KLogging

class LocalSuspendRateLimiterTest: AbstractSuspendRateLimiterTest() {

    companion object: KLogging()

    val bucketProvider by lazy {
        LocalSuspendBucketProvider(defaultBucketConfiguration)
    }

    override val rateLimiter: SuspendRateLimiter<String> by lazy {
        LocalSuspendRateLimiter(bucketProvider)
    }
}
