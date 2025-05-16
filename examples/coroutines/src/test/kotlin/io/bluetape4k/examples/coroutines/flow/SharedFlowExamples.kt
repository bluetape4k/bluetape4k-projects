package io.bluetape4k.examples.coroutines.flow

import app.cash.turbine.test
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * 복수의 collector 에게 동시에 데이터를 전달하는 SharedFlow 를 사용하는 예제
 */
class SharedFlowExamples {

    companion object: KLoggingChannel()

    /**
     * 복수의 Job에서 SharedFlow 에 Message를 전달할 수도 있고,
     * 복수의 Collector들이 같은 Message를 수신할 수 있다.
     */
    @Test
    fun `shared flow 기본 사용법`() = runTest {
        coroutineScope {
            val mutableSharedFlow = MutableSharedFlow<String>(replay = 0) // replay = 0: 캐시를 사용하지 않음
            val collected1 = atomic(0)
            val collected2 = atomic(0)

            launch {
                mutableSharedFlow
                    .log("#1")
                    .collect {
                        collected1.incrementAndGet()
                    }
            }

            launch {
                mutableSharedFlow
                    .log("#2")
                    .collect {
                        collected2.incrementAndGet()
                    }
            }

            yield()

            launch {
                // 2 개의 메시지를 보낸다
                mutableSharedFlow.emit("Message1")
                yield()
                mutableSharedFlow.emit("Message2")
                yield()

                // 자체적으로 collect 를 수행한다.
                mutableSharedFlow.test {
                    awaitItem() shouldBeEqualTo "Message1"
                    awaitItem() shouldBeEqualTo "Message2"
                    cancelAndConsumeRemainingEvents()
                }
            }
            delay(100)

            // 복수의 collector 들도 모두 같은 데이터를 수신한다.
            collected1.value shouldBeEqualTo 2
            collected2.value shouldBeEqualTo 2

            coroutineContext.cancelChildren()
        }
    }

    /**
     * SharedFlow 내부에 캐시를 사용해서 emit 이후에 collector 가 등록되더라도 이전에 emit 된 데이터 중 replay 숫자만큼 전달할 수 있다.
     */
    @Test
    fun `replay cache item in shared flow`() = runTest {
        val sharedFlow = MutableSharedFlow<String>(replay = 2)
        sharedFlow.emit("Message1")
        sharedFlow.emit("Message2")
        sharedFlow.emit("Message3")

        val collectCounter1 = atomic(0)
        val collectCounter2 = atomic(0)

        coroutineScope {
            sharedFlow.replayCache shouldBeEqualTo listOf("Message2", "Message3")

            launch {
                sharedFlow
                    .log("#1")      // Message 2, Message 3
                    .collect {
                        collectCounter1.incrementAndGet()
                    }
            }
            yield()
            launch {
                sharedFlow
                    .log("#2")      // Message 2, Message 3
                    .collect {
                        collectCounter2.incrementAndGet()
                    }
            }
            yield()

            launch {
                sharedFlow.test {
                    sharedFlow.resetReplayCache()           // 캐시를 초기화
                    sharedFlow.replayCache.shouldBeEmpty()
                    cancelAndConsumeRemainingEvents()
                }
            }

            delay(100)
            collectCounter1.value shouldBeEqualTo 2
            collectCounter2.value shouldBeEqualTo 2

            coroutineContext.cancelChildren()
        }
    }

    /**
     * 일반 flow 를 shared flow 로 변경하여, 복수의 collector 를 붙일 수 있다 (shareIn)
     *
     * - started: SharingStarted.Eagerly: flow 가 시작되면, collector 가 붙을 때까지 기다리지 않고, 바로 시작 - Hot Stream
     * - replay: 0: 캐시를 사용하지 않음
     *
     * 결과
     *
     * ```
     * (1 sec)
     * #1 A             // #1 collector 만 있음
     * (1 sec)
     * #1 B
     * #2 B             // #2 collector 가 붙음
     * (1 sec)
     * #1 C
     * #2 C
     * #3 C             // #3 collector 가 붙음
     * ```
     */
    @Test
    fun `shareIn - change flow to shared flow`() = runTest {
        val flow = flowOf("A", "B", "C")
            .onEach { delay(1000) }
            .log("source")

        val sharedFlow = flow.shareIn(this, started = SharingStarted.Eagerly, replay = 0)

        val counter1 = atomic(0)
        val counter2 = atomic(0)
        val counter3 = atomic(0)

        delay(500)
        launch {
            sharedFlow.log("#1")
                .collect { counter1.incrementAndGet() }
        }

        delay(1000)
        launch {
            sharedFlow.log("#2")
                .collect { counter2.incrementAndGet() }
        }

        delay(1000)
        launch {
            sharedFlow.log("#3")
                .collect { counter3.incrementAndGet() }
        }

        delay(5000)

        counter1.value shouldBeEqualTo 3
        counter2.value shouldBeEqualTo 2
        counter3.value shouldBeEqualTo 1

        coroutineContext.cancelChildren()
    }
}
