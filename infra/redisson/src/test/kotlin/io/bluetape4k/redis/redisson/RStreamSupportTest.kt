package io.bluetape4k.redis.redisson

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redissonClient
import kotlinx.coroutines.future.await
import io.bluetape4k.assertions.invoking
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.RStream
import org.redisson.api.stream.StreamCreateGroupArgs
import org.redisson.api.stream.StreamMessageId
import org.redisson.api.stream.StreamReadGroupArgs
import java.time.Duration

@DisplayName("RStreamSupport")
class RStreamSupportTest {

    companion object: KLogging()

    private fun newStream(): RStream<String, String> {
        return redissonClient.getStream(randomName())
    }

    @Test
    fun `streamAddArgsOf - 단일 key_value 로 StreamAddArgs 를 생성한다`() {
        val args = streamAddArgsOf("k1", "v1")
        args.shouldNotBeNull()

        val stream = newStream()
        val id = stream.add(args)
        id.shouldNotBeNull()
    }

    @Test
    fun `streamAddArgsOf - Pair vararg 로 StreamAddArgs 를 생성한다`() {
        val args = streamAddArgsOf("k1" to "v1", "k2" to "v2")

        val stream = newStream()
        val id = stream.add(args)
        id.shouldNotBeNull()

        val range = stream.range(10, StreamMessageId.MIN, StreamMessageId.MAX)
        range[id]!! shouldBeEqualTo mapOf("k1" to "v1", "k2" to "v2")
    }

    @Test
    fun `streamAddArgsOf - Map 으로 StreamAddArgs 를 생성한다`() {
        val entries = mapOf("a" to "1", "b" to "2", "c" to "3")
        val args = streamAddArgsOf(entries)

        val stream = newStream()
        val id = stream.add(args)

        val range = stream.range(10, StreamMessageId.MIN, StreamMessageId.MAX)
        range[id]!! shouldBeEqualTo entries
    }

    @Test
    fun `ackAllAsync - 빈 ids 는 IllegalArgumentException 을 던진다`() {
        val stream = newStream()
        invoking {
            stream.ackAllAsync("group-a", emptyList())
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `ackAllAsync - 빈 groupName 은 IllegalArgumentException 을 던진다`() {
        val stream = newStream()
        invoking {
            stream.ackAllAsync("", listOf(StreamMessageId.NEWEST))
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `ackAllAsync - 메시지 수신 후 ACK 이 정상 처리된다`() = runSuspendIO {
        val stream = newStream()
        val groupName = "group-${randomName()}"
        val consumerName = "consumer-1"

        val id1 = stream.add(streamAddArgsOf("k1", "v1"))
        val id2 = stream.add(streamAddArgsOf("k2", "v2"))

        stream.createGroup(StreamCreateGroupArgs.name(groupName).id(StreamMessageId.ALL))

        val messages = stream.readGroup(
            groupName,
            consumerName,
            StreamReadGroupArgs.neverDelivered()
        )
        messages.shouldHaveSize(2)

        val acked = stream.ackAllAsync(groupName, listOf(id1, id2)).await()
        acked shouldBeEqualTo 2L
    }

    @Test
    fun `claimAllAsync - 빈 ids 는 IllegalArgumentException 을 던진다`() {
        val stream = newStream()
        invoking {
            stream.claimAllAsync(
                groupName = "g",
                consumerName = "c",
                ids = emptyList()
            )
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `claimAllAsync - 빈 consumerName 은 IllegalArgumentException 을 던진다`() {
        val stream = newStream()
        invoking {
            stream.claimAllAsync(
                groupName = "g",
                consumerName = " ",
                ids = listOf(StreamMessageId.NEWEST)
            )
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `fastClaimAllAsync - 빈 ids 는 IllegalArgumentException 을 던진다`() {
        val stream = newStream()
        invoking {
            stream.fastClaimAllAsync(
                groupName = "g",
                consumerName = "c",
                ids = emptyList()
            )
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `claimAllAsync - 다른 소비자 메시지를 claim 한다`() = runSuspendIO {
        val stream = newStream()
        val groupName = "group-${randomName()}"

        val id1 = stream.add(streamAddArgsOf("k1", "v1"))
        stream.add(streamAddArgsOf("k2", "v2"))

        stream.createGroup(StreamCreateGroupArgs.name(groupName).id(StreamMessageId.ALL))

        // consumer-A 가 메시지를 읽어 pending 상태로 둠
        stream.readGroup(groupName, "consumer-A", StreamReadGroupArgs.neverDelivered())

        // consumer-B 가 claim
        val claimed = stream.claimAllAsync(
            groupName = groupName,
            consumerName = "consumer-B",
            idleTime = Duration.ZERO,
            ids = listOf(id1)
        ).await()

        (id1 in claimed.keys) shouldBeEqualTo true
    }

    @Test
    fun `fastClaimAllAsync - ID 리스트를 반환한다`() = runSuspendIO {
        val stream = newStream()
        val groupName = "group-${randomName()}"

        val id1 = stream.add(streamAddArgsOf("k1", "v1"))
        val id2 = stream.add(streamAddArgsOf("k2", "v2"))

        stream.createGroup(StreamCreateGroupArgs.name(groupName).id(StreamMessageId.ALL))

        stream.readGroup(groupName, "consumer-A", StreamReadGroupArgs.neverDelivered())

        val claimedIds = stream.fastClaimAllAsync(
            groupName = groupName,
            consumerName = "consumer-B",
            idleTime = Duration.ZERO,
            ids = listOf(id1, id2)
        ).await()

        claimedIds.size.shouldBeGreaterOrEqualTo(1)
    }
}
