package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.EncodingType
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class S3ModelSupportTest {

    @Test
    fun `putObjectRequestOf는 필드를 설정한다`() {
        val request = putObjectRequestOf(
            bucket = "bucket",
            key = "key",
            metadata = mapOf("k" to "v"),
            contentType = "text/plain",
        )

        request.bucket shouldBeEqualTo "bucket"
        request.key shouldBeEqualTo "key"
        request.metadata shouldBeEqualTo mapOf("k" to "v")
        request.contentType shouldBeEqualTo "text/plain"
    }

    @Test
    fun `listObjectsRequestOf는 필드를 설정한다`() {
        val request = listObjectsRequestOf(
            bucket = "bucket",
            prefix = "a/",
            delimiter = "/",
            maxKeys = 100,
            encondingType = EncodingType.Url,
        )

        request.bucket shouldBeEqualTo "bucket"
        request.prefix shouldBeEqualTo "a/"
        request.delimiter shouldBeEqualTo "/"
        request.maxKeys shouldBeEqualTo 100
        request.encodingType shouldBeEqualTo EncodingType.Url
    }

    @Test
    fun `objectIdentifierOf는 필드를 설정한다`() {
        val identifier = objectIdentifierOf(key = "key", versionId = "v1")

        identifier.key shouldBeEqualTo "key"
        identifier.versionId shouldBeEqualTo "v1"
    }

    @Test
    fun `deleteOf는 ObjectIdentifier 목록을 설정한다`() {
        val delete = deleteOf(listOf(objectIdentifierOf("key-1"), objectIdentifierOf("key-2")))

        delete.objects.size shouldBeEqualTo 2
        delete.objects.first().key shouldBeEqualTo "key-1"
    }

    @Test
    fun `deleteObjectRequestOf는 필드를 설정한다`() {
        val request = deleteObjectRequestOf(
            bucket = "bucket",
            key = "key",
            versionId = "v1",
        )

        request.bucket shouldBeEqualTo "bucket"
        request.key shouldBeEqualTo "key"
        request.versionId shouldBeEqualTo "v1"
    }

    @Test
    fun `deleteObjectsRequestOf는 식별자 목록을 설정한다`() {
        val identifiers = listOf(objectIdentifierOf("key-1"), objectIdentifierOf("key-2"))

        val request = deleteObjectsRequestOf(bucket = "bucket", identifiers = identifiers)

        request.bucket shouldBeEqualTo "bucket"
        request.delete?.objects?.size shouldBeEqualTo 2
    }

    @Test
    fun `copyObjectRequestOf는 원본과 대상을 설정한다`() {
        val request = copyObjectRequestOf(
            srcBucket = "src-bucket",
            srcKey = "src key",
            destBucket = "dest-bucket",
            destKey = "dest-key",
        )

        request.copySource shouldBeEqualTo "src-bucket%2Fsrc+key"
        request.bucket shouldBeEqualTo "dest-bucket"
        request.key shouldBeEqualTo "dest-key"
    }

    @Test
    fun `deleteOf는 빈 목록을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            deleteOf(emptyList<String>())
        }
    }

    @Test
    fun `deleteObjectsRequestOf는 빈 식별자 목록을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            deleteObjectsRequestOf(bucket = "bucket", identifiers = emptyList())
        }
    }
}
