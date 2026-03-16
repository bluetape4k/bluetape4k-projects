package io.bluetape4k.aws.s3

import io.bluetape4k.io.deleteIfExists
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.utils.Resourcex
import kotlinx.coroutines.future.await
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

@Execution(ExecutionMode.CONCURRENT)
class S3AsyncClientExtensionsTest: AbstractS3Test() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
    }

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `exists bucket async returns false for missing bucket`() = runSuspendIO {
        val missingBucket = "missing-${randomKey()}"

        s3AsyncClient.existsBucketAsync(missingBucket).await().shouldBeFalse()
    }

    @Test
    fun `put and get s3 object`() = runSuspendIO {
        val key = randomKey()
        val content = randomString()

        val response = s3AsyncClient.putAsStringAsync(BUCKET_NAME, key, content).await()
        response.eTag().shouldNotBeNull()
        log.debug { "put response=$response" }

        val downContent = s3AsyncClient.getAsStringAsync(BUCKET_NAME, key).await()
        downContent shouldBeEqualTo content
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `upload and download as byte array`() = runSuspendIO {
        val key = randomKey()
        val filepath = "files/product_type.csv"
        val bytes = Resourcex.getBytes(filepath)

        val response = s3AsyncClient.putAsByteArrayAsync(BUCKET_NAME, key, bytes).await()
        response.eTag().shouldNotBeNull()

        val download = s3AsyncClient.getAsByteArrayAsync(BUCKET_NAME, key).await()
        download.toUtf8String() shouldBeEqualTo bytes.toUtf8String()
    }

    @ParameterizedTest(name = "upload/download {0}")
    @MethodSource("getImageNames")
    fun `upload and download binary file`(filename: String) = runSuspendIO {
        val key = randomKey()
        val path = "$imageBasePath/$filename"
        val file = File(path)
        file.exists().shouldBeTrue()

        val response = s3AsyncClient.putAsFileAsync(BUCKET_NAME, key, file).await()
        response.eTag().shouldNotBeNull()

        val downloadFile = tempDir.resolve(filename)
        val response2 = s3AsyncClient.getAsFileAsync(BUCKET_NAME, key, downloadFile.toPath()).await()
        response2.eTag().shouldNotBeNull()

        log.debug { "downloadFile=$downloadFile, size=${downloadFile.length()}" }
        downloadFile.exists().shouldBeTrue()
        downloadFile.length() shouldBeEqualTo file.length()
        downloadFile.deleteIfExists()
    }
}
