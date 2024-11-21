package io.bluetape4k.examples.coroutines.dispatchers

import io.bluetape4k.coroutines.support.coLogging
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

@OptIn(DelicateCoroutinesApi::class)
class DispatcherExamples {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 1000
    }

    @Test
    fun `default dispatcher 예제`() = runTest {
        List(REPEAT_SIZE) {
            // Default dispatcher를 적용
            launch(Dispatchers.Default) {
                List(REPEAT_SIZE) { Random.nextLong() }.maxOrNull()

                // thread 는 cpu core 수 만큼 사용한다 
                // thread name 에 @coroutine#number 가 붙는다 
                val threadName = Thread.currentThread().name
                coLogging { "Running on thread $threadName" }
            }
        }.joinAll()
    }

    @Test
    fun `main dispatcher 설정하기`() = runTest {
        newSingleThreadContext("custom").use { dispatcher ->
            // main dispatcher를 설정합니다. 기본은 fork join을 사용하는 Dispatchers.Default 입니다.
            Dispatchers.setMain(dispatcher)
            try {
                // 지정하지 않으면 default dispatcher를 사용합니다. ( Dispatcher-worker-1 @coroutine#2 )
                // Dispatchers.IO 를 지정하면 DefaultDispatcher-worker-1 @coroutine#2 형태로 나타납니다.
                launch(Dispatchers.IO) {
                    val threadName = Thread.currentThread().name
                    coLogging { "thread name=$threadName" }
                    threadName shouldContain "DefaultDispatcher-worker"
                }.log("IO")
            } finally {
                Dispatchers.resetMain()
            }
        }
    }

    /**
     * IO Dispatcher 는 IO 작업 등 시간이 많이 걸리는 작업을 수행할 때 사용하기 위해 제공됩니다.
     *
     * 예를 들어, 파일작업, 공유자원 관리, blocking 함수 호출 등에 사용합니다.
     *
     * 기본적으로 64개의 Thread 를 가지고 있으며, [Dispatchers.IO.limitedParallelism()] 를 통해 변경할 수 있습니다.
     *
     * @see [Dispatchers.IO]
     */
    @Test
    fun `io dispatcher 사용 예`() = runTest {
        val jobs = List(REPEAT_SIZE) {
            // Dispatchers.IO.limitedParallelism(128)
            launch(Dispatchers.IO) {
                delay(Random.nextLong(100, 200))

                val threadName = Thread.currentThread().name
                coLogging { "Running on thread $threadName" }
            }
        }
        jobs.joinAll()
    }

    @Test
    fun `custom dispatcher 사용 예`() = runTest {
        newFixedThreadPoolContext(4, "custom").use { dispatcher ->
            // 동시 작업을 2개로 제한합니다.
            val parallel = dispatcher.limitedParallelism(2)
            List(REPEAT_SIZE) {
                launch(parallel) {
                    delay(Random.nextLong(100, 200))
                    val threadName = Thread.currentThread().name
                    coLogging { "Running on thread $threadName" }
                }
            }.joinAll()
        }
    }

    /**
     * Single thread에서 수행되므로 concurrency 가 보장됩니다.
     */
    @Test
    fun `dispatcher with single thread`() = runTest {
        newSingleThreadContext("single").use { dispatcher ->
            val counter = atomic(0)

            val jobs = List(REPEAT_SIZE) {
                launch(dispatcher) {
                    counter.incrementAndGet()
                    coLogging { "count=${counter.value}, thread=${Thread.currentThread().name}" }
                }
            }
            jobs.joinAll()
            counter.value shouldBeEqualTo REPEAT_SIZE
        }
    }

    /**
     * [Dispatchers.Unconfined] 는 Coroutines 가 Thread 에 종속되지 않습니다.
     * 사용하는 것을 추천하지 않습니다.
     */
    @Test
    fun `unconfiend dispatcher 사용 예`() = runTest {
        withContext(newSingleThreadContext("Name1")) {
            var continuation: Continuation<Unit>? = null

            val job2 = launch(newSingleThreadContext("Name2")) {
                delay(Random.nextLong(100, 200))
                continuation?.resume(Unit)
            }.log("job2")

            yield()

            // `Dispatchers.Unconfined` 를 사용하면 suspend 후 다른 Thread에서 실행됩니다.
            launch(Dispatchers.Unconfined) {
                coLogging { "thread=" + Thread.currentThread().name }   // Name 1
                Thread.currentThread().name shouldContain "Name1"

                suspendCoroutine { cont ->
                    continuation = cont
                }

                coLogging { "thread=" + Thread.currentThread().name }   // Name 2
                Thread.currentThread().name shouldContain "Name2"

                // Name2 job 종료
                delay(100)
                job2.join()

                coLogging { "thread=" + Thread.currentThread().name }   // DefaultExecutor
                Thread.currentThread().name shouldContain "DefaultExecutor"
            }.log("Unconfined")
                .join()
        }
    }
}
