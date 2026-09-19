package io.bluetape4k.retrofit2

import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.concurrent.sequence
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.retrofit2.clients.hc5.hc5CallFactoryOf
import io.bluetape4k.retrofit2.services.Httpbin
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.random.Random

class RetrofitSupportTest: AbstractRetrofitTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
        private const val CALL_SIZE = 100
    }

    private val jsonApi: Httpbin.HttpbinApi by lazy {
        retrofitOf(testBaseUrl, hc5CallFactoryOf()).service()
    }

    @Test
    fun `Retrofit용 API 생성`() {
        jsonApi.shouldNotBeNull()
    }

    @Test
    fun `warm up`() {
        val response = jsonApi.getPost(1).execute()
        log.debug { "response=$response" }
        response.isSuccessful.shouldBeTrue()
        response.body().shouldNotBeNull()
    }

    @Nested
    inner class Single {

        @Test
        fun `Retrofit용 API를 활용한 동기방식 호출`() {
            val response = jsonApi.getPost(1).execute()
            log.debug { "response=$response" }
            response.isSuccessful.shouldBeTrue()
            response.body().shouldNotBeNull()
        }

        @Test
        fun `Retrofit용 API를 활용한 비동기방식 호출`() {
            val future = jsonApi.getPost(1).executeAsync()

            val response = future.get()
            log.debug { "response=$response" }
            response.isSuccessful.shouldBeTrue()
            response.body().shouldNotBeNull()
        }

        @Test
        fun `Retrofit용 API를 활용한 Coroutines 호출`() = runSuspendIO {
            val response = jsonApi.getPost(1).executeAsync().await()

            log.debug { "response=$response" }
            response.isSuccessful.shouldBeTrue()
            response.body().shouldNotBeNull()
        }
    }

    @Nested
    inner class Bulk {

        @RepeatedTest(REPEAT_SIZE)
        fun `Retrofit용 API를 활용한 동기방식 Bulk 호출`() {
            val responses = List(CALL_SIZE) {
                jsonApi.getPost(Random.nextInt(1, 100)).execute()
            }
            responses.forEach { response ->
                log.debug { "response=${response}" }
                response.isSuccessful.shouldBeTrue()
                response.body().shouldNotBeNull()
            }
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `Retrofit용 API를 활용한 비동기방식 Bulk 호출`() {
            val futures = List(CALL_SIZE) {
                jsonApi
                    .getPost(Random.nextInt(1, 100))
                    .executeAsync()
            }
            val responses = futures.sequence(VirtualThreadExecutor).get()
            responses.forEach { response ->
                log.debug { "response=${response}" }
                response.isSuccessful.shouldBeTrue()
                response.body().shouldNotBeNull()
            }
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `Retrofit용 API를 활용한 Coroutines Bulk 호출`() = runSuspendIO {
            val responses = List(CALL_SIZE) { it }
                .asFlow()
                .async {
                    jsonApi
                        .getPost(Random.nextInt(1, 100))
                        .executeAsync()
                        .await()

                }
                .toList()

            responses.forEach { response ->
                log.debug { "response=${response}" }
                response.isSuccessful.shouldBeTrue()
                response.body().shouldNotBeNull()
            }
        }
    }
}
