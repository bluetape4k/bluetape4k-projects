package io.bluetape4k.http.hc5.fluent

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.http.httpHostOf
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import org.apache.hc.client5.http.fluent.Executor
import org.apache.hc.client5.http.fluent.Form
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpVersion
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test

/**
 * This example demonstrates how the he HttpClient fluent API can be used to execute multiple
 * requests within the same security context. The Executor class maintains a common context shared
 * by all requests executed with it. The Executor is thread-safe and can be used to execute
 * requests concurrently from multiple threads of execution.
 */
@TempFolderTest
class FluentExecutorExamples: AbstractHc5Test() {

    companion object: KLogging()

    private val executor: Executor = Executor.newInstance()
        .auth(httpHostOf(httpbinBaseUrl), "user", "passwd".toCharArray())
        .auth(httpHostOf("https://nghttp2.org"), "user", "passwd".toCharArray())
        .authPreemptive(httpHostOf(httpbinBaseUrl))

    @Test
    fun `get with timeout settings`() {
        // Execute a GET with timeout settings and return response content as String.
        val content = executor
            .execute(
                requestGet("$httpbinBaseUrl/basic-auth/user/passwd")
                    .connectTimeout(Timeout.ofSeconds(1))
            )
            .returnContent()
            .asString(Charsets.UTF_8)

        log.debug { "content=$content" }
    }

    @Test
    fun `post with HTTP 1_1`() {
        // Execute a POST with the 'expect-continue' handshake, using HTTP/1.1,
        // containing a request body as String and return response content as byte array.
        val contentBytes = executor
            .execute(
                requestPost("$httpbinBaseUrl/post")
                    .useExpectContinue()
                    .version(HttpVersion.HTTP_1_1)
                    .bodyString("Important stuff", ContentType.DEFAULT_TEXT)
            )
            .returnContent()
            .asBytes()

        log.debug { "contentBytes=${contentBytes.toUtf8String()}" }
    }

    @Test
    fun `post multi-part form data`(tempFolder: TempFolder) {
        // Execute a POST with a custom header through the proxy containing a request body
        // as an HTML form and save the result to the file
        // @see hc5/examples/ClientMultipartFormPost
        val path = tempFolder.createFile()
        executor
            .execute(
                requestPost("$httpbinBaseUrl/post")
                    .addHeader("X-Custom-Header", "stuff")
                    .bodyForm(
                        Form.form()
                            .add("username", "user")
                            .add("password", "secret")
                            .build()
                    )
            )
            .saveContent(path)
    }
}
