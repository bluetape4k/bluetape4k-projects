package io.bluetape4k.http.okhttp3.examples

import com.alibaba.fastjson2.toJSONByteArray
import com.alibaba.fastjson2.toJSONString
import io.bluetape4k.concurrent.onFailure
import io.bluetape4k.concurrent.onSuccess
import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.http.model.Post
import io.bluetape4k.http.okhttp3.CachingRequestInterceptor
import io.bluetape4k.http.okhttp3.CachingResponseInterceptor
import io.bluetape4k.http.okhttp3.bodyAsString
import io.bluetape4k.http.okhttp3.executeAsync
import io.bluetape4k.http.okhttp3.okhttp3Client
import io.bluetape4k.http.okhttp3.okhttp3RequestOf
import io.bluetape4k.http.okhttp3.print
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.writeAsString
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderExtension
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Duration
import java.util.concurrent.CompletionException
import java.util.concurrent.CountDownLatch
import kotlin.test.assertFailsWith

@RandomizedTest
class Recipes: AbstractHttpTest() {

    companion object: KLogging()

    private val client = okhttp3Client {
        retryOnConnectionFailure(true)
        readTimeout(Duration.ofSeconds(5))
    }

    private val mediaTypeMarkDown: MediaType = "text/x-markdown; charset=utf-8".toMediaTypeOrNull()!!

    private fun printHeaders(headers: Headers) {
        log.debug {
            buildString {
                appendLine("Headers:")
                headers.forEach { (name, value) ->
                    appendLine("  $name=$value")
                }
            }
        }
    }

    @Test
    fun `동기 방식 HTTP GET`() {
        val request = okhttp3RequestOf(HELLOWORLD_URL)
        val response = client.newCall(request).execute()

        assertResponse(response)
        printHeaders(response.headers)
        log.debug { response.bodyAsString() }
    }

    @Test
    fun `비동기 방식 HTTP GET`() {
        val lock = CountDownLatch(1)

        val request = okhttp3RequestOf(HELLOWORLD_URL)
        val callback = object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                lock.countDown()
            }

            override fun onResponse(call: Call, response: Response) {
                assertResponse(response)
                printHeaders(response.headers)
                log.debug { response.bodyAsString() }
                lock.countDown()
            }
        }
        client.newCall(request).enqueue(callback)
        lock.await()
    }

    @Test
    fun `동기방식 HTTP POST with String with Jackson`(@RandomValue post: Post) {
        val mapper = Jackson.defaultJsonMapper
        val json = mapper.writeAsString(post).orEmpty()

        val request = Request.Builder()
            .url("$JSON_PLACEHOLDER_URL/posts")
            .post(json.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val response = client.newCall(request).execute()

        assertResponse(response)
        log.debug { response.bodyAsString() }
    }

    @Test
    fun `동기방식 HTTP POST with ByteArray with Jackson`(@RandomValue post: Post) {
        val mapper = Jackson.defaultJsonMapper
        val json = mapper.writeValueAsBytes(post)

        val request = Request.Builder()
            .url("$JSON_PLACEHOLDER_URL/posts")
            .post(json.toRequestBody("application/json".toMediaTypeOrNull(), 0, json.size))
            .build()

        val response = client.newCall(request).execute()

        assertResponse(response)
        log.debug { response.bodyAsString() }
    }

    @Test
    fun `동기방식 HTTP POST with String with Fastjson2`(@RandomValue post: Post) {
        val json = post.toJSONString()

        val request = Request.Builder()
            .url("$JSON_PLACEHOLDER_URL/posts")
            .post(json.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val response = client.newCall(request).execute()

        assertResponse(response)
        log.debug { response.bodyAsString() }
    }

    @Test
    fun `동기방식 HTTP POST with ByteArray with Fastjson2`(@RandomValue post: Post) {
        val json = post.toJSONByteArray()

        val request = Request.Builder()
            .url("$JSON_PLACEHOLDER_URL/posts")
            .post(json.toRequestBody("application/json".toMediaTypeOrNull(), 0, json.size))
            .build()

        val response = client.newCall(request).execute()

        assertResponse(response)
        log.debug { response.bodyAsString() }
    }

    @Test
    @ExtendWith(TempFolderExtension::class)
    fun `Caching response`(tempFolder: TempFolder) {

        val cache = Cache(tempFolder.root, 1024 * 1024L).apply { initialize() }

        // Cache를 적용한 시스템입니다.
        val cachedClient = okhttp3Client {
            cache(cache)
            addInterceptor(CachingRequestInterceptor())  // 네트웍 요청 전 Interceptor
            addNetworkInterceptor(CachingResponseInterceptor())  // 네트웍 응답 후 Interceptor
        }

        val request = Request.Builder().url(HELLOWORLD_URL).build()

        val response1 = cachedClient.newCall(request).execute()
        assertResponse(response1)
        response1.print(1)
        printHeaders(response1.headers)

        // 캐시에서만 읽어오도록 한다
        val request2 = Request.Builder()
            .cacheControl(CacheControl.FORCE_CACHE)
            .url(HELLOWORLD_URL)
            .build()

        val response2 = cachedClient.newCall(request2).execute()
        // 캐시 읽기 실패하면 status=504 가 된다.
        response2.code shouldNotBeEqualTo 504
        response2.print(2)
        printHeaders(response2.headers)
        response2.bodyAsString()!!.shouldNotBeEmpty()

        val response3 = cachedClient.newCall(request2).execute()
        // 캐시 읽기 실패하면 status=504 가 된다.
        response3.code shouldNotBeEqualTo 504
        response3.print(3)
        printHeaders(response3.headers)
        response3.bodyAsString()!!.shouldNotBeEmpty()
    }

    @Test
    fun `요청 취소`() {
        // TODO : 구현해야 한다.
    }

    @Test
    fun `Timeout 발생 처리`() {
        val client = okhttp3Client {
            connectTimeout(Duration.ofSeconds(1))
            writeTimeout(Duration.ofSeconds(1))
            readTimeout(Duration.ofSeconds(1))
        }

        // 2 초간 지연을 시킵니다.
        val request = okhttp3RequestOf("$HTTPBIN_URL/delay/2")

        // 동기 방식에서의 Timeout 발생
        assertFailsWith<SocketTimeoutException> {
            client.newCall(request).execute()
        }
    }

    @Test
    fun `비동기 호출에 대한 Timeout 처리`() {
        val client = okhttp3Client {
            connectTimeout(Duration.ofSeconds(1))
            writeTimeout(Duration.ofSeconds(1))
            readTimeout(Duration.ofSeconds(1))
        }

        // 2 초간 지연을 시킵니다.
        val request = okhttp3RequestOf("$HTTPBIN_URL/delay/2")

        assertFailsWith<CompletionException> {
            client.executeAsync(request)
                .onSuccess { fail("Timeout 에러가 나야합니다.") }
                .onFailure { error -> error.cause shouldBeInstanceOf SocketTimeoutException::class }
                .join()
        }
    }
}
