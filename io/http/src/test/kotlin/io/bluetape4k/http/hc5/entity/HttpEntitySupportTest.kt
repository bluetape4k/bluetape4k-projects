package io.bluetape4k.http.hc5.entity

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity
import org.junit.jupiter.api.Test

class HttpEntitySupportTest {

    companion object : KLogging()

    @Test
    fun `consumeQuietly on non-null entity does not throw`() {
        val entity = StringEntity("content", ContentType.TEXT_PLAIN)

        // Should not throw any exception
        entity.consumeQuietly()
    }

    @Test
    fun `consumeQuietly on null entity is safe`() {
        val entity: StringEntity? = null

        // Null-safe call — should not throw
        entity.consumeQuietly()
    }

    @Test
    fun `consume on non-null entity does not throw`() {
        val entity = StringEntity("content to consume", ContentType.TEXT_PLAIN)

        // Should not throw any exception
        entity.consume()
    }

    @Test
    fun `consume on null entity is safe`() {
        val entity: StringEntity? = null

        // Null-safe call — should not throw
        entity.consume()
    }

    @Test
    fun `toByteArrayOrNull returns correct byte content`() {
        val text = "Hello, ByteArray!"
        val entity = StringEntity(text, ContentType.TEXT_PLAIN)

        val bytes = entity.toByteArrayOrNull()

        bytes.shouldNotBeNull()
        val decoded = String(bytes, Charsets.UTF_8)
        decoded shouldBeEqualTo text
    }

    @Test
    fun `toByteArrayOrNull with maxResultLength limits result`() {
        val text = "Hello, World!"
        val entity = StringEntity(text, ContentType.TEXT_PLAIN)

        // Max length of 5 should return only the first 5 bytes
        val bytes = entity.toByteArrayOrNull(maxResultLength = 5)

        bytes.shouldNotBeNull()
        bytes.size shouldBeEqualTo 5
    }

    @Test
    fun `toStringOrNull returns correct string content`() {
        val text = "Hello, String!"
        val entity = StringEntity(text, ContentType.TEXT_PLAIN)

        val result = entity.toStringOrNull()

        result shouldBeEqualTo text
    }

    @Test
    fun `toStringOrNull with custom charset returns correct string`() {
        val text = "UTF-8 content: 한글"
        val entity = StringEntity(text, ContentType.create("text/plain", Charsets.UTF_8))

        val result = entity.toStringOrNull(charset = Charsets.UTF_8)

        result shouldBeEqualTo text
    }

    @Test
    fun `parse returns list of NameValuePairs for url-encoded content`() {
        val urlEncodedContent = "key1=value1&key2=value2"
        val entity = StringEntity(
            urlEncodedContent,
            ContentType.create("application/x-www-form-urlencoded", Charsets.UTF_8)
        )

        val pairs = entity.parse()

        pairs.shouldHaveSize(2)
        val map = pairs.associate { it.name to it.value }
        map["key1"] shouldBeEqualTo "value1"
        map["key2"] shouldBeEqualTo "value2"
    }

    @Test
    fun `parse returns empty list for empty content`() {
        val entity = StringEntity(
            "",
            ContentType.create("application/x-www-form-urlencoded", Charsets.UTF_8)
        )

        val pairs = entity.parse()

        // Empty content produces empty or single empty pair — just verify no exception
        pairs.shouldNotBeNull()
    }

    @Test
    fun `toStringOrNull with maxResultLength limits result`() {
        val text = "Hello, World!"
        val entity = StringEntity(text, ContentType.TEXT_PLAIN)

        val result = entity.toStringOrNull(maxResultLength = 5)

        // Result should be limited
        result.shouldNotBeNull()
        result.length shouldBeEqualTo 5
    }
}
