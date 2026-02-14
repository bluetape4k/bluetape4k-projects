package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.classic.httpClientOf
import io.bluetape4k.http.hc5.entity.toStringOrNull
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.http.message.StatusLine
import org.junit.jupiter.api.Test

class ClientResponseProcessing: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `get response`() {
        val httpclient = httpClientOf()

        httpclient.use {
            val httpget = HttpGet("$httpbinBaseUrl/get")

            log.debug { "Executing request ${httpget.method} ${httpget.uri}" }

            val result = httpclient.execute(httpget) { response ->
                log.debug { "----------------" }
                log.debug { "$httpget -> " + StatusLine(response) }
                Result(response.code, response.entity.toStringOrNull())
            }
            log.debug { result }
            result.status shouldBeEqualTo 200
        }
    }

    @Test
    fun `get response in multi threading`() {
        val httpclient = httpClientOf()

        httpclient.use {

            MultithreadingTester()
                .workers(16)
                .rounds(2)
                .add {
                    val httpget = HttpGet("$httpbinBaseUrl/get")
                    log.debug { "Executing request ${httpget.method} ${httpget.uri}" }

                    val result = httpclient.execute(httpget) { response ->
                        log.debug { "----------------" }
                        log.debug { "$httpget -> " + StatusLine(response) }
                        Result(response.code, response.entity.toStringOrNull())
                    }
                    log.debug { result }
                    result.status shouldBeEqualTo 200
                }
                .run()
        }
    }

    data class Result(val status: Int, val content: String?)
}
