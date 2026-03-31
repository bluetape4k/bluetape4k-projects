package io.bluetape4k.aws.kotlin.s3

import aws.sdk.kotlin.services.s3.listBuckets
import io.bluetape4k.aws.kotlin.AbstractAwsTest
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll

abstract class AbstractKotlinS3Test: AbstractAwsTest() {

    companion object: KLoggingChannel() {
        const val IMAGE_PATH: String = "src/test/resources/images"
        const val BUCKET_NAME: String = "test-bucket-1"
        const val BUCKET_NAME2: String = "test-bucket-2"

        @JvmStatic
        protected fun getImageNames(): List<String> {
            return listOf(
                "cafe.jpg",
                "flower.jpg",
                "garden.jpg",
                "landscape.jpg",
                "wabull.jpg",
                "coroutines.pdf",
                "kotlin.key",
            )
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return io.bluetape4k.junit5.faker.Fakers.randomString(min, max)
        }

        @JvmStatic
        protected fun randomKey(): String = Base58.randomString(16).lowercase()
    }

    @BeforeAll
    fun beforeAll() {
        runBlocking {
            withS3Client(
                localStackServer.endpointUrl,
                localStackServer.region,
                localStackServer.credentialsProvider,
            ) { client ->
                log.info { "Delete all buckets ..." }
                val buckets = client.listBuckets { }.buckets!!
                buckets.forEach { bucket ->
                    client.forceDeleteBucket(bucket.name!!)
                }

                listOf(BUCKET_NAME, BUCKET_NAME2).forEach { bucketName ->
                    runCatching {
                        client.ensureBucketExists(bucketName)
                    }.onFailure {
                        log.warn(it) { "Failed to create bucket: $bucketName" }
                    }
                }
            }
        }
    }
}
