package io.bluetape4k.bucket4j.local

import io.bluetape4k.bucket4j.bucketConfiguration
import io.bluetape4k.logging.KLogging
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LocalBucketProviderTest: AbstractLocalBucketProviderTest() {

    companion object: KLogging()

    override val bucketProvider: LocalBucketProvider by lazy {
        val configuration = bucketConfiguration {
            addLimit {
                it.capacity(INITIAL_CAPACITY).refillIntervally(INITIAL_CAPACITY, 10.seconds.toJavaDuration())
            }
        }
        LocalBucketProvider(configuration)
    }

}
