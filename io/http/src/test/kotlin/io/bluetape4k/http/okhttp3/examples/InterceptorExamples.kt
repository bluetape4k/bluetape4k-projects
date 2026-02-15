package io.bluetape4k.http.okhttp3.examples

import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.http.okhttp3.LoggingInterceptor
import io.bluetape4k.http.okhttp3.okhttp3Client
import io.bluetape4k.http.okhttp3.okhttp3RequestOf
import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import io.bluetape4k.logging.KLogging
import okhttp3.OkHttpClient
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InterceptorExamples: AbstractHttpTest() {

    companion object: KLogging()

    private lateinit var appender: InMemoryLogbackAppender

    @BeforeEach
    fun beforeEach() {
        appender = InMemoryLogbackAppender()
    }

    @AfterEach
    fun afterEach() {
        appender.stop()
    }

    @Test
    fun `request interceptor`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(LoggingInterceptor(log))
            .build()

        val request = okhttp3RequestOf(HELLOWORLD_URL)
        val response = client.newCall(request).execute()

        response.body.shouldNotBeNull()
        appender.lastMessage!! shouldStartWith "Receive response. url=$HELLOWORLD_URL"
    }

    @Test
    fun `response interceptor`() {
        val client = okhttp3Client {
            // redirect 로그까지 남는다 (http -> https)
            addNetworkInterceptor(LoggingInterceptor(log))
        }

        val request = okhttp3RequestOf(HELLOWORLD_URL)
        val response = client.newCall(request).execute()

        response.body.shouldNotBeNull()
        appender.lastMessage!! shouldStartWith "Receive response. url=$HELLOWORLD_URL"
    }
}
