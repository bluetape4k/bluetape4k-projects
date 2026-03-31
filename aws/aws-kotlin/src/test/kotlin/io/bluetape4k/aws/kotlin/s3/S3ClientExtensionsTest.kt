package io.bluetape4k.aws.kotlin.s3

import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.decodeToString
import io.bluetape4k.aws.kotlin.s3.model.getObjectRequestOf
import io.bluetape4k.aws.kotlin.s3.model.putObjectRequestOf
import io.bluetape4k.io.deleteIfExists
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import kotlin.time.Duration.Companion.seconds

@TempFolderTest
class S3ClientExtensionsTest: AbstractKotlinS3Test() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `upload and download s3 object as String`() = runSuspendIO {
        withS3Client(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val key = randomKey()
            val content = randomString()

            val response = client.putFromString(BUCKET_NAME, key, content)
            response.eTag.shouldNotBeNull()
            log.debug { "Put response=$response" }

            val downloadedContent = client.getAsString(BUCKET_NAME, key)
            downloadedContent.shouldNotBeNull() shouldBeEqualTo content
        }
    }

    @Test
    fun `existsBucket는 없는 버킷에 대해 false를 반환한다`() = runSuspendIO {
        withS3Client(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val missingBucket = "missing-${randomKey()}"
            client.existsBucket(missingBucket).shouldBeFalse()
        }
    }

    @Test
    fun `existsObject는 없는 객체에 대해 false를 반환한다`() = runSuspendIO {
        withS3Client(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val missingKey = "missing-${randomKey()}"
            client.existsObject(BUCKET_NAME, missingKey).shouldBeFalse()
        }
    }

    @Test
    fun `putAll은 모든 요청을 실행하고 업로드 결과를 반환한다`() = runSuspendIO {
        withS3Client(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val prefix = "bulk-put-${randomKey()}"
            val requests = arrayOf(
                putObjectRequestOf(BUCKET_NAME, "$prefix-1", body = ByteStream.fromString("alpha")),
                putObjectRequestOf(BUCKET_NAME, "$prefix-2", body = ByteStream.fromString("beta")),
                putObjectRequestOf(BUCKET_NAME, "$prefix-3", body = ByteStream.fromString("gamma")),
            )

            val responses = client.putAll(concurrency = 2, *requests).toList()
            responses.size shouldBeEqualTo requests.size
            client.existsObject(BUCKET_NAME, "$prefix-1").shouldBeTrue()
            client.existsObject(BUCKET_NAME, "$prefix-2").shouldBeTrue()
            client.existsObject(BUCKET_NAME, "$prefix-3").shouldBeTrue()
        }
    }

    @Test
    fun `getAll은 모든 요청을 실행하고 객체 본문을 반환한다`() = runSuspendIO {
        withS3Client(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val prefix = "bulk-get-${randomKey()}"
            val samples = listOf("one", "two", "three")
            samples.forEachIndexed { index, value ->
                client.putFromString(BUCKET_NAME, "$prefix-$index", value)
            }

            val requests = samples.indices.map { index ->
                getObjectRequestOf(BUCKET_NAME, "$prefix-$index")
            }.toTypedArray()

            val contents = client
                .getAll(concurrency = 2, *requests)
                .buffer()
                .map { it.body?.decodeToString() }
                .toList()

            contents.size shouldBeEqualTo samples.size
            contents shouldBeEqualTo samples

            delay(1.seconds)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `upload and download s3 object as ByteArray`() = runSuspendIO {
        withS3Client(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val key = randomKey()
            val content = randomString().toUtf8Bytes()

            val response = client.putFromByteArray(BUCKET_NAME, key, content)
            response.eTag.shouldNotBeNull()
            log.debug { "Put response=$response" }

            val downloadedContent = client.getAsByteArray(BUCKET_NAME, key)
            downloadedContent.shouldNotBeNull() shouldBeEqualTo content
        }
    }

    @ParameterizedTest(name = "upload/download {0}")
    @MethodSource("getImageNames")
    fun `upload and download binary file`(filename: String, tempFolder: TempFolder) = runSuspendIO {
        withS3Client(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val key = randomKey()
            val filepath = "$IMAGE_PATH/$filename"
            val file = File(filepath)

            val response = client.putFromFile(BUCKET_NAME, key, file)
            response.eTag.shouldNotBeNull()

            val downloadFile = tempFolder.createFile()
            val downloadLength = client.getAsFile(BUCKET_NAME, key, downloadFile)

            log.debug { "Download file=$downloadFile, length=${downloadFile.length()}" }
            with(downloadFile) {
                exists().shouldBeTrue()
                length() shouldBeEqualTo downloadLength
                length() shouldBeEqualTo file.length()
                deleteIfExists()
            }
        }
    }

    @ParameterizedTest(name = "multijob up/down {0}")
    @MethodSource("getImageNames")
    fun `upload and download binary file in multi jobs`(
        filename: String,
        tempFolder: TempFolder,
    ) = runSuspendIO {
        withS3Client(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            SuspendedJobTester()
                .workers(Runtimex.availableProcessors)
                .rounds(Runtimex.availableProcessors)
                .add {
                    val key = randomKey()
                    val filepath = "$IMAGE_PATH/$filename"
                    val file = File(filepath)

                    val response = client.putFromFile(BUCKET_NAME, key, file)
                    response.eTag.shouldNotBeNull()

                    val downloadFile = tempFolder.createFile()
                    val downloadLength = client.getAsFile(BUCKET_NAME, key, downloadFile)

                    log.debug { "Download file=$downloadFile, length=${downloadFile.length()}" }
                    with(downloadFile) {
                        exists().shouldBeTrue()
                        length() shouldBeEqualTo downloadLength
                        length() shouldBeEqualTo file.length()
                        deleteIfExists()
                    }
                }
                .run()
        }
    }
}
