package io.bluetape4k.bucket4j.local

import io.bluetape4k.bucket4j.bucketConfiguration
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LocalCoBucketProviderTest: AbstractLocalBucketProviderTest() {

    companion object: KLoggingChannel()

    override val bucketProvider: AbstractLocalBucketProvider by lazy {
        val configuration = bucketConfiguration {
            addLimit {
                it.capacity(10).refillIntervally(10, 10.seconds.toJavaDuration())
            }
        }
        LocalCoBucketProvider(configuration)
    }
}
