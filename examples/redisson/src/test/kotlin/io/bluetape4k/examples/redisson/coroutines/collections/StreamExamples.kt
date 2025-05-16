package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.coroutines.support.coAwait
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.ackAllAsync
import io.bluetape4k.redis.redisson.streamAddArgsOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.redisson.api.RStream
import org.redisson.api.StreamMessageId
import org.redisson.api.stream.StreamCreateGroupArgs
import org.redisson.api.stream.StreamReadGroupArgs
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * [RStream] Examples
 *
 * 참고: [Redis Stream for Java](https://redisson.org/articles/redis-streams-for-java.html)
 */
class StreamExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `stream 기본 사용 예`() {
        val groupName = "testGroup-" + TimebasedUuid.Reordered.nextIdAsString()

        val stream = redisson.getStream<String, String>(randomName())

        // Consumer group 을 만든다
        stream.createGroup(StreamCreateGroupArgs.name(groupName).makeStream())

        val id1 = stream.add(streamAddArgsOf("key1", "value1"))
        log.debug { "메시지 전송, messageId1=$id1" }
        val id2 = stream.add(streamAddArgsOf("key2", "value2"))
        log.debug { "메시지 전송, messageId2=$id2" }

        val group = stream.readGroup(
            groupName,
            "consumer1",
            StreamReadGroupArgs.neverDelivered()
        )
        group.forEach { (id, map) ->
            log.debug { "Read group. id=$id, map=$map" }
        }

        // return entries in pending state after read group method execution
        val pendingData = stream.pendingRange(
            groupName,
            "consumer1",
            StreamMessageId.MIN,
            StreamMessageId.MAX,
            100
        )
        pendingData.forEach { (id, map) ->
            log.debug { "Pending data. id=$id, map=$map" }
        }

        // transfer ownership of pending messages to a new consumer
        val transferedIds = stream.fastClaim(
            groupName,
            "consumer2",
            1,
            TimeUnit.MILLISECONDS,
            id1,
            id2
        )
        transferedIds.forEach { id ->
            log.debug { "Transfered id=$id" }
        }

        // mark pending entries as correctly processed
        val amount = stream.ack(groupName, id1, id2)

        amount shouldBeEqualTo 2L
    }

    @Test
    fun `stream usage`() = runSuspendIO {
        val groupName = "group-" + TimebasedUuid.Reordered.nextIdAsString()
        val consumerName1 = "consumer-" + TimebasedUuid.Reordered.nextIdAsString()
        val consumerName2 = "consumer-" + TimebasedUuid.Reordered.nextIdAsString()

        val stream: RStream<String, Int> = redisson.getStream(randomName())

        // Consumer group 을 만든다
        stream.createGroup(StreamCreateGroupArgs.name(groupName).makeStream())

        // 1번째 메시지를 전송한다 (Pair 전송)
        val mId1 = stream.addAsync(streamAddArgsOf("1", 1)).coAwait()
        log.debug { "메시지 전송, mId1=$mId1" }

        // 2번째 메시지를 전송한다 (Pair 전송)
        val mId2 = stream.addAsync(streamAddArgsOf("2", 2)).coAwait()
        log.debug { "메시지 전송, mId2=$mId2" }

        // 2개의 메시지를 받는다
        val group = stream.readGroupAsync(
            groupName,
            consumerName1,
            StreamReadGroupArgs.neverDelivered()
        ).coAwait()

        group.forEach { (mid, body) ->
            log.debug { "메시지 수신, mid=$mid, body=$body" }
        }

        group.keys shouldHaveSize 2
        group.keys shouldBeEqualTo setOf(mId1, mId2)

        // 2개의 메시지를 읽었다고 ack 보냄 (전송완료)
        stream.ackAsync(groupName, *group.keys.toTypedArray()).coAwait()

        // 메시지를 기다린다.
        val consumerJob = launch {
            // 1개의 메시지를 받는다
            val group2 = stream.readGroupAsync(
                groupName,
                consumerName2,
                StreamReadGroupArgs.neverDelivered().timeout(10.seconds.toJavaDuration())
            ).coAwait()

            // 1개의 메시지를 받았다
            group2.keys shouldHaveSize 1
            val msgId = group2.keys.first()
            log.debug { "메시지 수신, msgId=$msgId, body=${group2[msgId]}" }
            group2[msgId]!! shouldBeEqualTo mapOf<String, Int>("3" to 3, "4" to 4)

            stream.ackAllAsync(groupName, group2.keys).coAwait() shouldBeEqualTo 1L
        }

        // 새로운 메시지 1개를 전송한다 (단 body 자체가 map 형태이다)
        val mId3 = stream.addAsync(streamAddArgsOf("3" to 3, "4" to 4)).coAwait()
        log.debug { "메시지 전송, mId3=$mId3" }
        delay(10)
        consumerJob.join()

        stream.deleteAsync().coAwait()
    }
}
