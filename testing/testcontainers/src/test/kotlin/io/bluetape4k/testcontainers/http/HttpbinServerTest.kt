package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.net.HttpURLConnection
import java.net.URI

@Execution(ExecutionMode.SAME_THREAD)
class HttpbinServerTest {

    companion object: KLogging()

    @Test
    fun `launch Httpbin server`() {
        HttpbinServer().use { httpbin ->
            httpbin.start()
            httpbin.isRunning.shouldBeTrue()

            callHttpbinServer(httpbin)
        }
    }

    @Test
    fun `launch Httpbin server with default port`() {
        HttpbinServer(useDefaultPort = true).use { httpbin ->
            httpbin.start()
            httpbin.isRunning.shouldBeTrue()

            httpbin.port shouldBeEqualTo HttpbinServer.PORT

            callHttpbinServer(httpbin)
        }
    }

    private fun callHttpbinServer(httpbin: HttpbinServer) {
        val baseUrl = httpbin.url
        val url = URI("$baseUrl/ip").toURL()

        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.addRequestProperty("Accept", "*/*")

            conn.responseCode shouldBeEqualTo 200
            val response = conn.inputStream.reader().buffered().readText()
            log.debug { "response=$response" }
            response shouldContain "origin"
        } finally {
            conn.disconnect()
        }
    }
}
