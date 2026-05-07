package io.bluetape4k.http.hc5.entity

import io.bluetape4k.http.hc5.http.ContentTypes
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotBeNullOrEmpty
import org.apache.hc.core5.http.ContentType
import org.junit.jupiter.api.Test

class EntityBuilderTest {

    companion object : KLogging()

    @Test
    fun `httpEntity DSL block creates StringEntity with text`() {
        val entity = httpEntity {
            setText("Hello, World!")
            setContentType(ContentTypes.TEXT_PLAIN_UTF8)
        }

        entity.shouldNotBeNull()
        val content = entity.toStringOrNull()
        content shouldBeEqualTo "Hello, World!"
    }

    @Test
    fun `httpEntityOf with text creates entity with correct content`() {
        val text = "Hello from httpEntityOf"
        val entity = httpEntityOf(text = text)

        entity.shouldNotBeNull()
        val content = entity.toStringOrNull()
        content shouldBeEqualTo text
    }

    @Test
    fun `httpEntityOf with text sets content type`() {
        val entity = httpEntityOf(
            text = "sample text",
            contentType = ContentTypes.TEXT_PLAIN_UTF8,
        )

        entity.shouldNotBeNull()
        val contentType = entity.contentType
        contentType.shouldNotBeNullOrEmpty()
        contentType shouldContain "text/plain"
    }

    @Test
    fun `httpEntityOf with ByteArray creates entity with correct bytes`() {
        val bytes = "binary content".toByteArray(Charsets.UTF_8)
        val entity = httpEntityOf(
            binary = bytes,
            contentType = ContentType.DEFAULT_BINARY,
        )

        entity.shouldNotBeNull()
        val content = entity.toByteArrayOrNull()
        content.shouldNotBeNull()
        // ByteArray uses reference equality — compare decoded string
        String(content, Charsets.UTF_8) shouldBeEqualTo String(bytes, Charsets.UTF_8)
    }

    @Test
    fun `httpEntityOf with ByteArray verifies byte content matches`() {
        val original = byteArrayOf(1, 2, 3, 4, 5)
        val entity = httpEntityOf(binary = original)

        entity.shouldNotBeNull()
        val result = entity.toByteArrayOrNull()
        result.shouldNotBeNull()
        result.size shouldBeEqualTo original.size
        result.toList() shouldBeEqualTo original.toList()
    }

    @Test
    fun `httpEntity DSL with binary content creates entity with correct bytes`() {
        val bytes = byteArrayOf(10, 20, 30)
        val entity = httpEntity {
            setBinary(bytes)
            setContentType(ContentType.DEFAULT_BINARY)
        }

        entity.shouldNotBeNull()
        val content = entity.toByteArrayOrNull()
        content.shouldNotBeNull()
        content.size shouldBeEqualTo bytes.size
        content.toList() shouldBeEqualTo bytes.toList()
    }

    @Test
    fun `toByteArrayOrNull returns correct byte content`() {
        val text = "Test content"
        val entity = httpEntityOf(text = text, contentType = ContentTypes.TEXT_PLAIN_UTF8)

        val bytes = entity.toByteArrayOrNull()
        bytes.shouldNotBeNull()
        val decoded = String(bytes, Charsets.UTF_8)
        decoded shouldBeEqualTo text
    }

    @Test
    fun `toStringOrNull returns correct string content`() {
        val text = "String content test"
        val entity = httpEntityOf(text = text)

        val result = entity.toStringOrNull()
        result shouldBeEqualTo text
    }
}
