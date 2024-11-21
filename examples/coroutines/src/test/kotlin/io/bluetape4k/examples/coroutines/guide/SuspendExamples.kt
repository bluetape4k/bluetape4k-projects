package io.bluetape4k.examples.coroutines.guide

import io.bluetape4k.concurrent.onFailure
import io.bluetape4k.concurrent.onSuccess
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

class SuspendExamples {

    companion object: KLogging()

    class Service {

        fun executeAsync(delayMillis: Long): CompletableFuture<Int> {
            return CompletableFuture.supplyAsync {
                Thread.sleep(delayMillis)
                42
            }
        }

        /**
         * CompletableFuture 를 반환하는 비동기 함수를 suspend 함수로 변환합니다.
         *
         * @param delayMillis
         * @return
         */
        suspend fun executeWithDelay(delayMillis: Long): Int {
            return suspendCancellableCoroutine { cont ->
                executeAsync(delayMillis)
                    .onSuccess { result ->
                        log.trace { "Completed result=$result" }
                        cont.resume(result) { cancellation ->
                            log.trace { "Cancel suspend" }
                            cont.cancel(cancellation)
                        }
                    }
                    .onFailure { error ->
                        log.trace { "cause error!!!" }
                        cont.resumeWithException(error)
                    }
            }
        }
    }

    private val service = Service()

    @Test
    fun `executeAsync with delay`() {
        service.executeAsync(Random.nextLong(10)).get() shouldBeEqualTo 42
    }

    @Test
    fun `execute suspend function`() = runSuspendTest {
        val result = service.executeWithDelay(Random.nextLong(10))
        result shouldBeEqualTo 42
    }

    @Test
    fun `execute suspend function with timeout`() = runSuspendTest {
        // 지정된 시간 안에 결과가 없다면 null을 반환합니다.
        val result = withTimeoutOrNull(10) {
            service.executeWithDelay(1000)
        }
        result.shouldBeNull()
    }
}
