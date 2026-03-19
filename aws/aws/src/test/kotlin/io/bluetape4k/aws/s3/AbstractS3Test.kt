package io.bluetape4k.aws.s3

import io.bluetape4k.aws.AbstractAwsTest
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.junit.jupiter.api.BeforeAll
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.transfer.s3.S3TransferManager

abstract class AbstractS3Test: AbstractAwsTest() {
    companion object: KLoggingChannel() {
        const val IMAGE_PATH: String = "./src/test/resources/images"
        const val BUCKET_NAME: String = "test-bucket"
        const val BUCKET_NAME2: String = "test-bucket-2"

        @JvmStatic
        protected fun randomKey(): String = Base58.randomString(16).lowercase()
    }

    val s3Client: S3Client by lazy {
        S3ClientFactory.Sync.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    val s3AsyncClient: S3AsyncClient by lazy {
        S3ClientFactory.Async.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient
        )
    }

    val s3TransferManager: S3TransferManager by lazy {
        S3ClientFactory.TransferManager.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
        ) {
            this.executor(Dispatchers.IO.asExecutor())
        }
    }

    @BeforeAll
    fun beforeAll() {
        deleteBuckets()
        createBucketsIfNotExists(BUCKET_NAME, BUCKET_NAME2)
    }

    protected fun deleteBuckets() {
        // 기존 bucket을 삭제한다
        val buckets = s3Client.listBuckets().buckets()

        buckets.forEach { bucket ->
            val listObjects = s3Client.listObjects { it.bucket(bucket.name()) }.contents()
            listObjects
                .map { it.key() }
                .forEach { key ->
                    log.debug { "Delete object... bucket=${bucket.name()}, key=$key" }
                    s3Client.deleteObject { it.bucket(bucket.name()).key(key) }
                }
            log.debug { "Delete bucket... name=${bucket.name()}" }
            s3Client.deleteBucket { it.bucket(bucket.name()) }
        }
    }

    protected fun createBucketsIfNotExists(vararg bucketNames: String) {
        bucketNames.forEach { bucket ->
            if (s3Client.existsBucket(bucket).getOrDefault(false).not()) {
                log.debug { "Create bucket... name=$bucket" }
                runCatching {
                    val response = s3Client.createBucket { it.bucket(bucket) }
                    if (response.sdkHttpResponse().isSuccessful) {
                        log.debug { "Create new bucket. name=$bucket, location=${response.location()}" }
                    } else {
                        log.error { "Fail to create bucket. name=$bucket, response=${response.sdkHttpResponse()}" }
                    }
                }
            }
        }
    }

    protected val imageBasePath: String = "src/test/resources/images"

    protected fun getImageNames(): List<String> =
        listOf(
            "cafe.jpg",
            "flower.jpg",
            "garden.jpg",
            "landscape.jpg",
            "wabull.jpg",
            "coroutines.pdf",
            "kotlin.key",
        )
}
