package io.bluetape4k.aws.s3.transfer

import io.bluetape4k.aws.s3.AbstractS3Test
import io.bluetape4k.codec.Base58
import io.bluetape4k.io.deleteIfExists
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import kotlinx.coroutines.future.await
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

class S3TransferManagerTest: AbstractS3Test() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
    }

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `upload and download text by transfer manager`() = runSuspendIO {
        val key = Base58.randomString(16)
        val content = randomString()

        val upload = s3TransferManager.uploadByteArray(BUCKET_NAME, key, content.toUtf8Bytes())

        val completedUpload = upload.completionFuture().await()
        completedUpload.response().eTag().shouldNotBeEmpty()

        val download = s3TransferManager.downloadAsByteArray(BUCKET_NAME, key)

        val completedDownload = download.completionFuture().await()

        val downloadContent = completedDownload.result().asByteArray().toUtf8String()
        downloadContent shouldBeEqualTo content
    }

    @ParameterizedTest(name = "upload/download by transfer manager: {0}")
    @MethodSource("getImageNames")
    fun `upload and download file by transfer manager`(filename: String) = runSuspendIO {
        val key = "transfer/$filename"
        val path = "$imageBasePath/$filename"
        val file = File(path)
        file.exists().shouldBeTrue()

        // Upload a file by S3TransferManager
        val upload = s3TransferManager.uploadFile(BUCKET_NAME, key, file.toPath())
        val completedUpload = upload.completionFuture().await()
        completedUpload.response().eTag().shouldNotBeEmpty()

        // TempDir 에 파일을 다운로드 한다
        val downloadFile = File(tempDir, filename)
        val downloadPath = downloadFile.toPath()

        val download = s3TransferManager.downloadFile(BUCKET_NAME, key, downloadPath)
        download.completionFuture().await()

        log.debug { "downloadFile=$downloadFile, size=${downloadFile.length()}" }
        downloadFile.exists().shouldBeTrue()
        // downloadFile.length() shouldBeEqualTo file.length()
        downloadFile.deleteIfExists()
    }
}
