package io.bluetape4k.examples.coroutines.select

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * [select] 표현식을 사용하여 복수의 suspend 연산 중 가장 먼저 완료된 것을 선택하는 예제입니다.
 *
 * - `onAwait`: 복수의 `async` 중 먼저 완료된 결과 선택
 * - `onReceive`: 복수의 Channel 중 먼저 데이터가 도착한 채널에서 수신
 * - `onSend`: 여유가 있는 채널에 먼저 전송
 */
class SelectExamples {
    companion object: KLoggingChannel()

    /**
     * 복수의 `async` 중 가장 먼저 완료된 결과를 선택합니다.
     *
     * 예: 여러 서버에 동시 요청 후 가장 빠른 응답을 사용하는 패턴
     */
    @Test
    fun `select onAwait - 가장 빠른 async 결과 선택`() = runTest {
        val slow = async {
            delay(200.milliseconds)
            "느린 응답"
        }

        val fast = async {
            delay(100.milliseconds)
            "빠른 응답"
        }

        // 먼저 수행한 결과를 선택
        val result = select {
            fast.onAwait { it }
            slow.onAwait { it }
        }

        log.debug { "선택된 결과: $result" }
        result shouldBeEqualTo "빠른 응답"

        // 나머지 코루틴도 정리
        fast.cancel()
        slow.cancel()
    }

    /**
     * 복수의 Channel 중 먼저 데이터가 도착한 채널에서 수신합니다.
     *
     * `select` 후 사용하지 않는 채널의 남은 요소를 `cancel()`로 정리해야 합니다.
     */
    @Test
    fun `select onReceive - 먼저 도착한 채널에서 수신`() = runTest {
        val channel1 = produce {
            delay(200.milliseconds)
            send("Channel 1 데이터")
        }
        val channel2 = produce {
            delay(100.milliseconds)
            send("Channel 2 데이터")
        }

        val result = select {
            channel1.onReceive { it }
            channel2.onReceive { it }
        }

        log.debug { "수신 결과: $result" }
        result shouldBeEqualTo "Channel 2 데이터"

        // 사용하지 않은 produce 코루틴 정리
        channel1.cancel()
        channel2.cancel()
    }

    /**
     * 복수의 채널을 순회하며 먼저 수신 가능한 채널에서 데이터를 가져옵니다.
     *
     * 채널이 닫힌 후에는 `onReceiveCatching`을 사용하여 안전하게 처리합니다.
     */
    @Test
    fun `select를 반복하여 복수 채널에서 번갈아 수신`() = runTest {
        val channel1 =
            produce {
                repeat(3) {
                    delay(150.milliseconds)
                    send("A$it")
                }
            }
        val channel2 =
            produce {
                repeat(3) {
                    delay(100.milliseconds)
                    send("B$it")
                }
            }

        val received = mutableListOf<String>()
        // 채널이 닫힐 수 있으므로 onReceiveCatching 사용
        repeat(6) {
            val result =
                select {
                    channel1.onReceiveCatching { it.getOrNull() }
                    channel2.onReceiveCatching { it.getOrNull() }
                }
            if (result != null) {
                received.add(result)
                log.debug { "수신: $result" }
            }
        }

        log.debug { "전체 수신: $received" }
        received shouldContain "A0"
        received shouldContain "B0"

        // 남은 produce 코루틴 정리
        channel1.cancel()
        channel2.cancel()
    }

    /**
     * [select]와 [Channel.onSend]를 사용하여 여유 있는 채널에 먼저 전송합니다.
     */
    @Test
    fun `select onSend - 여유 있는 채널에 전송`() = runTest {
        val channel1 = Channel<String>(1)
        val channel2 = Channel<String>(1)

        // channel1에 미리 데이터를 넣어 가득 차게 만듦
        channel1.send("기존 데이터")

        // channel1은 가득 차 있으므로 channel2에 전송됨
        select {
            channel1.onSend("새 데이터") { log.debug { "channel1에 전송" } }
            channel2.onSend("새 데이터") { log.debug { "channel2에 전송" } }
        }

        val result = channel2.receive()
        result shouldBeEqualTo "새 데이터"
        log.debug { "channel2에서 수신: $result" }

        channel1.close()
        channel2.close()
    }
}
