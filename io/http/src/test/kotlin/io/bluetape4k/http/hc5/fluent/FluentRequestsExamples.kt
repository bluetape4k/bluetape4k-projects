package io.bluetape4k.http.hc5.fluent

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import org.apache.hc.client5.http.fluent.Form
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpVersion
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test

/**
 * This example demonstrates basics of request execution with the HttpClient fluent API.
 */
@TempFolderTest
class FluentRequestsExamples: AbstractHc5Test() {

    companion object: KLogging()

    // Execute a GET with timeout settings and return response content as String.
    @Test
    fun `get with timeout settings and return content as string`() {
        val content = requestGet("$httpbinBaseUrl/get")
            .connectTimeout(Timeout.ofSeconds(1))
            .responseTimeout(Timeout.ofSeconds(5))
            .execute()
            .returnContent()
            .asString()

        log.debug { "content=$content" }
    }

    // Execute a POST with the 'expect-continue' handshake, using HTTP/1.1,
    // containing a request body as String and return response content as byte array.
    @Test
    fun `post with expect-continue hadshake using HTTP 1_1`() {
        val content = requestPost("$httpbinBaseUrl/post")
            .useExpectContinue()
            .version(HttpVersion.HTTP_1_1)
            .bodyString("Important stuff", ContentType.DEFAULT_TEXT)
            .execute()
            .returnContent()
            .asBytes()

        log.debug { "content=${content.toUtf8String()}" }
    }

    // Execute a POST with a custom header through the proxy containing a request body
    // as an HTML form and save the result to the file
    @Test
    fun `post with a custom header and form data and save response to file`(tempFolder: TempFolder) {
        requestPost("$httpbinBaseUrl/post")
            .addHeader("X-Custom-Header", "stuff")
            .bodyForm(
                Form.form()
                    .add("username", "vip")
                    .add("password", "secret")
                    .build()
            )
            .execute()
            .saveContent(tempFolder.createFile())
    }
}
