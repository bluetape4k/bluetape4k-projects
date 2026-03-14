package io.bluetape4k.exposed.core.statements.api

import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ExposedBlobExtensionsTest {
    @Test
    fun `String toExposedBlob and toUtf8String are reversible`() {
        val text = "hello exposed"
        val blob = text.toExposedBlob()

        blob.toUtf8String() shouldBeEqualTo text
    }

    @Test
    fun `빈 문자열을 ExposedBlob으로 변환하면 bytes 가 비어 있다`() {
        val blob = "".toExposedBlob()

        blob.toUtf8String() shouldBeEqualTo ""
        blob.toByteArray().toList().shouldBeEmpty()
    }

    @Test
    fun `ByteArray toExposedBlob and toByteArray are reversible`() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val blob = bytes.toExposedBlob()

        blob.toByteArray().toList() shouldBeEqualTo bytes.toList()
    }

    @Test
    fun `빈 ByteArray를 ExposedBlob으로 변환하면 bytes 가 비어 있다`() {
        val blob = ByteArray(0).toExposedBlob()

        blob.toByteArray().toList().shouldBeEmpty()
    }

    @Test
    fun `InputStream toExposedBlob preserves content`() {
        val bytes = "stream-data".toByteArray()
        val blob = bytes.inputStream().toExposedBlob()

        blob.toInputStream().readBytes().toList() shouldBeEqualTo bytes.toList()
    }

    @Test
    fun `빈 InputStream을 ExposedBlob으로 변환하면 bytes 가 비어 있다`() {
        val blob = ByteArray(0).inputStream().toExposedBlob()

        blob.toByteArray().toList().shouldBeEmpty()
    }

    @Test
    fun `toInputStream 은 blob bytes 를 그대로 읽을 수 있다`() {
        val bytes = byteArrayOf(10, 20, 30)
        val blob = bytes.toExposedBlob()

        blob.toInputStream().readBytes().toList() shouldBeEqualTo bytes.toList()
    }
}
