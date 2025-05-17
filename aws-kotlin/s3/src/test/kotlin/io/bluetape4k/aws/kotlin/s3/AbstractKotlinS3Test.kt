package io.bluetape4k.aws.kotlin.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.listBuckets
import io.bluetape4k.aws.kotlin.http.defaultCrtHttpEngineOf
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import io.bluetape4k.utils.ShutdownQueue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.localstack.LocalStackContainer

abstract class AbstractKotlinS3Test {

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
        val s3Server: LocalStackContainer by lazy {
            getLocalStackServer(LocalStackContainer.Service.S3)
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return io.bluetape4k.junit5.faker.Fakers.randomString(min, max)
        }
    }

    protected val s3Client: S3Client = S3Client {
        credentialsProvider = s3Server.getCredentialsProvider()
        endpointUrl = s3Server.endpointUrl
        region = s3Server.region
        httpClient = defaultCrtHttpEngineOf()
    }.apply {
        log.info { "S3Client created with endpoint: ${s3Server.endpoint}" }

        // JVM 종료 시 S3Client를 닫습니다.
        ShutdownQueue.register(this)
    }

    @BeforeAll
    fun beforeAll() {
        runBlocking {
            deleteAllBuckets()
            createBucketsIfNotExists(BUCKET_NAME, BUCKET_NAME2)
        }
    }

    protected suspend fun deleteAllBuckets() {
        log.info { "Delete all buckets ..." }
        val buckets = s3Client.listBuckets { }.buckets!!
        buckets.forEach { bucket ->
            s3Client.forceDeleteBucket(bucket.name!!)
        }
    }

    protected suspend fun createBucketsIfNotExists(vararg bucketNames: String) {
        bucketNames.forEach { bucketName ->
            runCatching {
                s3Client.ensureBucketExists(bucketName)
            }.onFailure {
                log.warn(it) { "Failed to create bucket: $bucketName" }
            }
        }
    }
}
