package io.bluetape4k.cache.memoizer.cache2k

import io.bluetape4k.cache.cache2k.cache2k
import io.bluetape4k.cache.memoizer.AbstractSuspendMemoizerTest
import io.bluetape4k.cache.memoizer.SuspendFactorialProvider
import io.bluetape4k.cache.memoizer.SuspendFibonacciProvider
import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cache2k.Cache
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class Cache2KSuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLoggingChannel()

    private val cache: Cache<Int, Int> = cache2k<Int, Int> {
        this.name("suspend-heavy-func")
        this.executor(Executors.newVirtualThreadPerTaskExecutor())
    }.build()

    override val heavyFunc: suspend (Int) -> Int = cache.suspendMemoizer {
        delay(100L.milliseconds)
        it * it
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        private val cache = cache2k<Long, Long> {
            this.name("suspend-factorial")
            this.executor(Executors.newVirtualThreadPerTaskExecutor())
        }.build()

        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }

    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        private val cache = cache2k<Long, Long> {
            this.name("suspend-fibonacci")
            this.executor(Executors.newVirtualThreadPerTaskExecutor())
        }.build()

        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }

    @Test
    fun `evaluator 실패 후 같은 키를 다시 호출하면 새 계산으로 복구된다`() = runSuspendDefault {
        val localCache = cache2k<String, Int> {
            this.name("suspend-recovery-${System.nanoTime()}")
            this.executor(Executors.newVirtualThreadPerTaskExecutor())
        }.build()
        val evalCount = AtomicInteger(0)
        val memo = localCache.suspendMemoizer { key: String ->
            if (evalCount.incrementAndGet() == 1) {
                error("transient failure")
            }
            key.length
        }

        assertFailsWith<IllegalStateException> {
            memo("recover")
        }

        memo("recover") shouldBeEqualTo 7
        evalCount.get() shouldBeEqualTo 2
        localCache.close()
    }

    @Test
    fun `clear 중 진행 중인 suspend 결과는 캐시에 저장되지 않는다`() = runSuspendDefault {
        val evalStarted = CompletableDeferred<Unit>()
        val evalProceed = CompletableDeferred<Unit>()
        val evalCount = AtomicInteger(0)

        val localCache = cache2k<String, Int> {
            this.name("write-after-clear-${System.nanoTime()}")
            this.executor(Executors.newVirtualThreadPerTaskExecutor())
        }.build()

        val memo: SuspendMemoizer<String, Int> = localCache.suspendMemoizer { key: String ->
            evalCount.incrementAndGet()
            evalStarted.complete(Unit)
            evalProceed.await()
            key.length
        }

        val job = launch { memo("hello") }
        evalStarted.await()     // wait for evaluator to start

        memo.clear()             // invalidate while in-flight
        evalProceed.complete(Unit)  // let evaluator finish
        job.join()

        // Cache must be empty — the in-flight result should not have been written back
        // Calling again triggers a fresh evaluation
        memo("hello") shouldBeEqualTo 5
        evalCount.get() shouldBeEqualTo 2  // re-evaluated because cache was empty after clear

        localCache.close()
    }
}
