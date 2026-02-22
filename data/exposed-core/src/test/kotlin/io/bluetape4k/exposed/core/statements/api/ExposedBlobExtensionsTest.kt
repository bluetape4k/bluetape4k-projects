package io.bluetape4k.exposed.core.statements.api

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
    fun `ByteArray toExposedBlob and toByteArray are reversible`() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val blob = bytes.toExposedBlob()

        blob.toByteArray().toList() shouldBeEqualTo bytes.toList()
    }

    @Test
    fun `InputStream toExposedBlob preserves content`() {
        val bytes = "stream-data".toByteArray()
        val blob = bytes.inputStream().toExposedBlob()

        blob.toInputStream().readBytes().toList() shouldBeEqualTo bytes.toList()
    }
}
