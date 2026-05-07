package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class ContentTypesTest {

    companion object : KLogging()

    @Test
    fun `TEXT_PLAIN_UTF8 MIME 타입 검증`() {
        val contentType = ContentTypes.TEXT_PLAIN_UTF8

        contentType.shouldNotBeNull()
        contentType.mimeType shouldBeEqualTo "text/plain"
    }

    @Test
    fun `TEXT_PLAIN_UTF8 charset UTF-8 검증`() {
        val contentType = ContentTypes.TEXT_PLAIN_UTF8

        contentType.shouldNotBeNull()
        contentType.charset shouldBeEqualTo Charsets.UTF_8
    }

    @Test
    fun `TEXT_PLAIN_UTF8 toString 형식 검증`() {
        val contentType = ContentTypes.TEXT_PLAIN_UTF8

        contentType.shouldNotBeNull()
        // mimeType과 charset 모두 포함
        val str = contentType.toString()
        str.contains("text/plain") shouldBeEqualTo true
        str.contains("UTF-8") shouldBeEqualTo true
    }
}
