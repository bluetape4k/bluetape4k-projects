package io.bluetape4k.http.okhttp3

import io.bluetape4k.http.okhttp3.mock.baseUrl
import io.bluetape4k.http.okhttp3.mock.enqueueBody
import io.bluetape4k.junit5.coroutines.runSuspendIO
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OkHttp3ClientExtensionsCoroutinesTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun beforeEach() {
        server = MockWebServer().apply { start() }
        client = okhttp3Client { }
    }

    @AfterEach
    fun afterEach() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        server.shutdown()
    }

    @Test
    fun `okhttp3RequestOf with vararg headers applies all headers`() {
        val request = okhttp3RequestOf(
            "${server.baseUrl}todos",
            "X-App-Key", "app-123",
            "X-Trace-Id", "trace-1"
        ) {
            get()
        }

        assertEquals("app-123", request.header("X-App-Key"))
        assertEquals("trace-1", request.header("X-Trace-Id"))
    }

    @Test
    fun `OkHttpClient executeSuspending returns successful response`() = runSuspendIO {
        server.enqueueBody("""{"result":"ok"}""", "Content-Type: application/json")
        val request = okhttp3Request {
            url(server.baseUrl)
            get()
        }

        client.executeSuspending(request).use { response ->
            assertTrue(response.isSuccessful)
            assertEquals("""{"result":"ok"}""", response.bodyAsString())
        }
    }

    @Test
    fun `Call executeSuspending returns successful response`() = runSuspendIO {
        server.enqueueBody("ok")
        val request = okhttp3Request {
            url(server.baseUrl)
            get()
        }
        val call = client.newCall(request)

        call.executeSuspending().use { response ->
            assertTrue(response.isSuccessful)
            assertEquals("ok", response.bodyAsString())
        }
    }

    @Test
    fun `deprecated suspendExecute api is still supported`() = runSuspendIO {
        server.enqueueBody("legacy")
        val request = okhttp3Request {
            url(server.baseUrl)
            get()
        }

        @Suppress("DEPRECATION")
        client.suspendExecute(request).use { response ->
            assertTrue(response.isSuccessful)
            assertNotNull(response.bodyAsString())
        }
    }
}
