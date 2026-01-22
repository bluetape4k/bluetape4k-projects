package io.bluetape4k.aws.kotlin.s3

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.io.deleteIfExists
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

@TempFolderTest
class S3ClientExtensionsTest: AbstractKotlinS3Test() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `upload and download s3 object as String`() = runSuspendIO {
        val key = TimebasedUuid.Reordered.nextIdAsString().lowercase()
        val content = randomString()

        val response = s3Client.putFromString(BUCKET_NAME, key, content)
        response.eTag.shouldNotBeNull()
        log.debug { "Put response=$response" }

        val downloadedContent = s3Client.getAsString(BUCKET_NAME, key)
        downloadedContent.shouldNotBeNull() shouldBeEqualTo content
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `upload and download s3 object as ByteArray`() = runSuspendIO {
        val key = TimebasedUuid.Reordered.nextIdAsString().lowercase()
        val content = randomString().toUtf8Bytes()

        val response = s3Client.putFromByteArray(BUCKET_NAME, key, content)
        response.eTag.shouldNotBeNull()
        log.debug { "Put response=$response" }

        val downloadedContent = s3Client.getAsByteArray(BUCKET_NAME, key)
        downloadedContent.shouldNotBeNull() shouldBeEqualTo content
    }

    @ParameterizedTest(name = "upload/download {0}")
    @MethodSource("getImageNames")
    fun `upload and download binary file`(filename: String, tempFolder: TempFolder) = runSuspendIO {
        val key = TimebasedUuid.Reordered.nextIdAsString().lowercase()
        val filepath = "$IMAGE_PATH/$filename"
        val file = File(filepath)

        val response = s3Client.putFromFile(BUCKET_NAME, key, file)
        response.eTag.shouldNotBeNull()

        val downloadFile = tempFolder.createFile()
        val downloadLength = s3Client.getAsFile(BUCKET_NAME, key, downloadFile)

        log.debug { "Download file=$downloadFile, length=${downloadFile.length()}" }
        with(downloadFile) {
            exists().shouldBeTrue()
            length() shouldBeEqualTo downloadLength
            length() shouldBeEqualTo file.length()
            deleteIfExists()
        }
    }

    @ParameterizedTest(name = "multijob up/down {0}")
    @MethodSource("getImageNames")
    fun `upload and download binary file in multi jobs`(
        filename: String,
        tempFolder: TempFolder,
    ) = runSuspendIO {
        SuspendedJobTester()
            .numThreads(Runtimex.availableProcessors)
            .roundsPerJob(Runtimex.availableProcessors)
            .add {
                val key = TimebasedUuid.Reordered.nextIdAsString().lowercase()
                val filepath = "$IMAGE_PATH/$filename"
                val file = File(filepath)

                val response = s3Client.putFromFile(BUCKET_NAME, key, file)
                response.eTag.shouldNotBeNull()

                val downloadFile = tempFolder.createFile()
                val downloadLength = s3Client.getAsFile(BUCKET_NAME, key, downloadFile)

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
