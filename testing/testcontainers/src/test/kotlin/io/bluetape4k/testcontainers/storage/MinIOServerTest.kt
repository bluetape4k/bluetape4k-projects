package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.StatObjectArgs
import io.minio.UploadObjectArgs
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNullOrBlank
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import kotlin.test.assertFailsWith

class MinIOServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `launch minio server`() {
            MinIOServer().use { minio ->
                minio.start()
                minio.isRunning.shouldBeTrue()
                minio.url.shouldNotBeNullOrBlank()
                minio.userName shouldBeEqualTo MinIOServer.DEFAULT_USER
                minio.password shouldBeEqualTo MinIOServer.DEFAULT_PASSWORD

                useBucket(minio)
            }
        }

        @Test
        fun `launch minio server with custom credentials`() {
            MinIOServer(username = "testuser", password = "testpassword").use { minio ->
                minio.start()
                minio.isRunning.shouldBeTrue()
                minio.url.shouldNotBeNullOrBlank()
                minio.userName shouldBeEqualTo "testuser"
                minio.password shouldBeEqualTo "testpassword"

                useBucket(minio)
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch minio server with default port`() {
            MinIOServer(useDefaultPort = true).use { minio ->
                minio.start()
                minio.isRunning.shouldBeTrue()
                minio.url.shouldNotBeNullOrBlank()
                minio.userName shouldBeEqualTo MinIOServer.DEFAULT_USER
                minio.password shouldBeEqualTo MinIOServer.DEFAULT_PASSWORD
                minio.port shouldBeEqualTo MinIOServer.S3_PORT

                useBucket(minio)
            }
        }
    }

    private fun useBucket(minio: MinIOServer) {
        val testBucketName = "test-bucket-" + Base58.randomString(4).lowercase()

        val client = MinIOServer.Launcher.getClient(minio)
        val makeArgs = MakeBucketArgs.builder().bucket(testBucketName).region("us-west-2").build()
        client.makeBucket(makeArgs)

        val existsArgs = BucketExistsArgs.builder().bucket(testBucketName).build()
        client.bucketExists(existsArgs).shouldBeTrue()

        val file = javaClass.classLoader.getResource("minio/cafe.jpg")!!
        val objectName = "test-object-name-" + Base58.randomString(4).lowercase()

        val uploadArgs = UploadObjectArgs.builder()
            .bucket(testBucketName)
            .`object`(objectName)
            .filename(file.path)
            .build()

        log.debug { "Upload object: bucket=$testBucketName, object=$objectName" }
        client.uploadObject(uploadArgs)

        val statObjectArgs = StatObjectArgs.builder()
            .bucket(testBucketName)
            .`object`(objectName)
            .build()

        val objectStat = client.statObject(statObjectArgs)
        objectStat.`object`() shouldBeEqualTo objectName
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { MinIOServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { MinIOServer(tag = " ") }
    }
}
