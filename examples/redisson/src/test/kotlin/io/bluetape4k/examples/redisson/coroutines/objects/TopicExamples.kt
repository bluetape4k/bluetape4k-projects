package io.bluetape4k.examples.redisson.coroutines.objects

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.awaitility.coUntil
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.redisson.client.codec.StringCodec


class TopicExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `add topic listener`() = runSuspendIO {
        val topic = redisson.getTopic(randomName(), StringCodec.INSTANCE)
        val receivedCounter = atomic(0)
        val receivedCount by receivedCounter

        // topic 예 listener를 등록합니다.
        // listener id 를 반환한다.
        val listenerId1 = topic.addListenerAsync(String::class.java) { channel, msg ->
            println("Listener1: channel[$channel] received: $msg")
            receivedCounter.incrementAndGet()
        }.suspendAwait()

        val listenerId2 = topic.addListenerAsync(String::class.java) { channel, msg ->
            println("Listener2: channel[$channel] received: $msg")
            receivedCounter.incrementAndGet()
        }.suspendAwait()

        log.debug { "Listener listener1 Id=$listenerId1, listener2 Id=$listenerId2" }

        // topic 에 메시지 전송
        topic.publishAsync("message-1").suspendAwait()
        topic.publishAsync("message-2").suspendAwait()

        // topic 에 listener가 2개, 메시지 2개 전송
        await coUntil { receivedCount == 2 * 2 }

        topic.removeAllListenersAsync().suspendAwait()
    }
}
