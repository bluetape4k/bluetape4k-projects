package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class HttpbinHttp2ServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `launch HttpbinHttp2 server`() {
        HttpbinHttp2Server().use { httpbin ->
            httpbin.start()
            httpbin.isRunning.shouldBeTrue()

            callHttpbinHttp2Server(httpbin)
        }
    }

    @Test
    fun `launch HttpbinHttp2 server with default port`() {
        HttpbinHttp2Server(useDefaultPort = true).use { httpbin ->
            httpbin.start()
            httpbin.isRunning.shouldBeTrue()

            httpbin.port shouldBeEqualTo HttpbinHttp2Server.PORT

            callHttpbinHttp2Server(httpbin)
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { HttpbinHttp2Server(image = " ") }
        assertFailsWith<IllegalArgumentException> { HttpbinHttp2Server(tag = " ") }
    }

    private fun callHttpbinHttp2Server(httpbin: HttpbinHttp2Server) {
        val client = OkHttpClient.Builder()
            .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
            .build()

        val request = Request.Builder()
            .get()
            .url("${httpbin.url}/get")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            log.debug { "protocol=${response.protocol}, code=${response.code}, response=$body" }

            response.isSuccessful.shouldBeTrue()
            (response.protocol == Protocol.H2_PRIOR_KNOWLEDGE || response.protocol == Protocol.HTTP_2).shouldBeTrue()
            body shouldContain "url"
        }
    }
}
