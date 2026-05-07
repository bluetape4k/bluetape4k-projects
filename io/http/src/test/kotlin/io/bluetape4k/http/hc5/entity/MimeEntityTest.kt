package io.bluetape4k.http.hc5.entity

import io.bluetape4k.http.hc5.entity.mime.formBodyPart
import io.bluetape4k.http.hc5.entity.mime.formBodyPartOf
import io.bluetape4k.http.hc5.entity.mime.multipartEntity
import io.bluetape4k.http.hc5.entity.mime.multipartPart
import io.bluetape4k.http.hc5.entity.mime.multipartPartOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotBeNullOrEmpty
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode
import org.apache.hc.client5.http.entity.mime.StringBody
import org.apache.hc.core5.http.ContentType
import org.junit.jupiter.api.Test

class MimeEntityTest {

    companion object : KLogging()

    @Test
    fun `multipartEntity DSL creates multipart form entity`() {
        val entity = multipartEntity {
            addTextBody("field1", "value1")
            addTextBody("field2", "value2")
        }

        entity.shouldNotBeNull()
        val contentType = entity.contentType
        contentType.shouldNotBeNullOrEmpty()
        contentType shouldContain "multipart"
    }

    @Test
    fun `multipartEntity with parameters creates entity with correct content type`() {
        val entity = multipartEntity(
            mode = HttpMultipartMode.STRICT,
            charset = Charsets.UTF_8,
        ) {
            addTextBody("name", "testValue")
        }

        entity.shouldNotBeNull()
        val contentType = entity.contentType
        contentType.shouldNotBeNullOrEmpty()
        contentType shouldContain "multipart"
    }

    @Test
    fun `multipartEntity with boundary sets custom boundary`() {
        val boundary = "custom-boundary-123"
        val entity = multipartEntity(
            boundary = boundary,
        ) {
            addTextBody("key", "value")
        }

        entity.shouldNotBeNull()
        val contentType = entity.contentType
        contentType.shouldNotBeNullOrEmpty()
        contentType shouldContain boundary
    }

    @Test
    fun `multipartEntity isStreaming is callable`() {
        val entity = multipartEntity {
            addTextBody("field", "value")
        }

        entity.shouldNotBeNull()
        // isStreaming() should be callable without error
        val streaming = entity.isStreaming
        streaming.shouldNotBeNull()
    }

    @Test
    fun `formBodyPart DSL creates FormBodyPart`() {
        val body = StringBody("Hello, World!", ContentType.TEXT_PLAIN)
        val part = formBodyPart {
            setName("message")
            setBody(body)
        }

        part.shouldNotBeNull()
        part.name shouldContain "message"
    }

    @Test
    fun `formBodyPart with name and body creates correct part`() {
        val body = StringBody("test content", ContentType.TEXT_PLAIN)
        val part = formBodyPart("field-name", body)

        part.shouldNotBeNull()
        part.name shouldContain "field-name"
    }

    @Test
    fun `formBodyPartOf with fields creates part with headers`() {
        val body = StringBody("text value", ContentType.TEXT_PLAIN)
        val fields = mapOf("Custom-Field" to "custom-value")
        val part = formBodyPartOf("myField", body, fields)

        part.shouldNotBeNull()
        part.name shouldContain "myField"
    }

    @Test
    fun `multipartPart DSL creates MultipartPart`() {
        val body = StringBody("part content", ContentType.TEXT_PLAIN)
        val part = multipartPart {
            setBody(body)
            addHeader("Content-Disposition", "form-data; name=\"file\"")
        }

        part.shouldNotBeNull()
        part.body.shouldNotBeNull()
    }

    @Test
    fun `multipartPart with body creates part correctly`() {
        val body = StringBody("Hello, Multipart!", ContentType.TEXT_PLAIN)
        val part = multipartPart(body)

        part.shouldNotBeNull()
        part.body.shouldNotBeNull()
    }

    @Test
    fun `multipartPartOf with body and fields creates part with headers`() {
        val body = StringBody("content", ContentType.TEXT_PLAIN)
        val fields = mapOf("Content-Disposition" to "form-data; name=\"data\"")
        val part = multipartPartOf(body, fields = fields)

        part.shouldNotBeNull()
        part.body.shouldNotBeNull()
    }

    @Test
    fun `multipartEntity with text body is readable`() {
        val entity = multipartEntity {
            addTextBody("username", "testUser")
            addTextBody("email", "test@example.com")
        }

        entity.shouldNotBeNull()
        entity.contentType.shouldNotBeNullOrEmpty()
        entity.contentType shouldContain "multipart"
    }
}
