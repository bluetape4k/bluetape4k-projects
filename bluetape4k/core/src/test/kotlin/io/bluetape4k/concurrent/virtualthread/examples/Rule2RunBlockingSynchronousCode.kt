package io.bluetape4k.concurrent.virtualthread.examples

import io.bluetape4k.concurrent.virtualthread.AbstractVirtualThreadTest
import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.amshove.kluent.`should be in range`
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * ### Rule 2
 *
 * 동기 코드를 비동기 방식으로 실행하는 추천 방식
 *
 * - 동기 방식의 Legacy 코드를 비동기로 실행할 때
 *      - CPU intensive 한 작업은 기존의 Platform Thread 를 사용하자
 *      - IO intensive 한 작업은 Virtual Thread 를 사용하자
 *      - Request-Response 방식에는 Virtual Thread 가 적합하다
 *
 *  - 신규 제작 시에는 Kotlin Coroutines 를 활용하는 방법도 있다.
 *      - 신규 제작 함수를 suspend 함수로 작성하고, Coroutine 을 이용하여 실행한다.
 */
class Rule2RunBlockingSynchronousCode: AbstractVirtualThreadTest() {

    companion object: KLogging()

    /**
     * CPU 를 많이 쓰는 작업은 기존의 Platform Thread 를 사용하는 CompletableFuture로 실행하는 것을 추천합니다.
     */
    @Test
    fun `동기 코드를 Platform Thread를 사용하는 CompletableFuture로 실행하기`() {
        val startMs = System.currentTimeMillis()

        CompletableFuture
            .supplyAsync { readPriceInEur() }
            .thenCombine(CompletableFuture.supplyAsync { readExchangeRateEurToUsd() }) { price, rate -> price * rate }
            .thenCompose { amount -> CompletableFuture.supplyAsync { amount * (1 + readTax(amount)) } }
            .whenComplete { grossAmountInUsd, error ->
                if (error == null) {
                    grossAmountInUsd.toInt() shouldBeEqualTo 108
                } else {
                    fail(error)
                }
            }
            .get()

        val durationMs = System.currentTimeMillis() - startMs
        log.debug { "비동기 코드 실행 시간 (msec): $durationMs" }
        durationMs `should be in range` 500L..1500L
    }

    /**
     * CPU intensive 가 아닌 IO 작업 위주인 경우, Request-Response 형태인 경우에는 Virtual Thread 를 사용하는 것을 추천합니다.
     */
    @Test
    fun `동기 코드를 Virtual Thread 로 실행하기`() {
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val startMs = System.currentTimeMillis()

            val priceInEur = executor.submit<Int> { readPriceInEur() }
            val exchangeRateEurToUsd = executor.submit<Float> { readExchangeRateEurToUsd() }
            val netAmountInUsd = priceInEur.get() * exchangeRateEurToUsd.get()

            val tax = executor.submit<Float> { readTax(netAmountInUsd) }
            val grossAmountInUsd = netAmountInUsd * (1 + tax.get())

            grossAmountInUsd.toInt() shouldBeEqualTo 108

            val duration = System.currentTimeMillis() - startMs
            log.debug { "Virtual Thread로 동기 코드 실행: $duration" }
        }
    }

    @Test
    fun `동기 코드를 VirtualFuture 로 실행하기`() {

        val startMs = System.currentTimeMillis()

        val priceInEur = virtualFuture<Int> { readPriceInEur() }
        val exchangeRateEurToUsd = virtualFuture<Float> { readExchangeRateEurToUsd() }
        val netAmountInUsd = priceInEur.await() * exchangeRateEurToUsd.await()

        val tax = virtualFuture<Float> { readTax(netAmountInUsd) }
        val grossAmountInUsd = netAmountInUsd * (1 + tax.get())

        grossAmountInUsd.toInt() shouldBeEqualTo 108

        val duration = System.currentTimeMillis() - startMs
        log.debug { "VirtualFuture로 동기 코드 실행: $duration" }
    }

    @Test
    fun `Default dispatcher를 이용하여 suspend 함수로 실행하기`() = runSuspendTest(Dispatchers.Default) {
        val startMs = System.currentTimeMillis()

        val priceInEur = async { readPriceInEurAwait() }
        val exchangeRateEurToUsd = async { readExchangeRateEurToUsdAwait() }
        val netAmoundInUsd = async { priceInEur.await() * exchangeRateEurToUsd.await() }

        val grossAmountInUsd = netAmoundInUsd.await() * (1 + readTaxAwait(netAmoundInUsd.await()))

        grossAmountInUsd.toInt() shouldBeEqualTo 108

        val duration = System.currentTimeMillis() - startMs
        log.debug { "Default Coroutines 실행 시간 (msec): $duration" }
    }

    @Test
    fun `Virtual thread dispatcher를 이용하여 suspend 함수로 실행하기`() = runSuspendTest(Dispatchers.VT) {
        val startMs = System.currentTimeMillis()

        val priceInEur = async { readPriceInEurAwait() }
        val exchangeRateEurToUsd = async { readExchangeRateEurToUsdAwait() }
        val netAmoundInUsd = async { priceInEur.await() * exchangeRateEurToUsd.await() }

        val grossAmountInUsd = netAmoundInUsd.await() * (1 + readTaxAwait(netAmoundInUsd.await()))

        grossAmountInUsd.toInt() shouldBeEqualTo 108

        val duration = System.currentTimeMillis() - startMs
        log.debug { "Virtual Thread Coroutines 실행 시간 (msec): $duration" }
    }

    private fun readPriceInEur(): Int {
        return sleepAndGet(200, 82)
    }

    private fun readExchangeRateEurToUsd(): Float {
        return sleepAndGet(300, 1.1f)
    }


    @Suppress("UNUSED_PARAMETER")
    private fun readTax(amount: Float): Float {
        return sleepAndGet(500, 0.2f)
    }

    private suspend fun readPriceInEurAwait(): Int {
        return sleepAndAwait(200, 82)
    }

    private suspend fun readExchangeRateEurToUsdAwait(): Float {
        return sleepAndAwait(300, 1.1f)
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun readTaxAwait(amount: Float): Float {
        return sleepAndAwait(500, 0.2f)
    }
}
