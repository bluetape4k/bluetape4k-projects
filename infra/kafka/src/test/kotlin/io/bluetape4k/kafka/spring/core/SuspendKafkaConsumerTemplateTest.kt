package io.bluetape4k.kafka.spring.core

import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.mq.KafkaServer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.consumer.OffsetAndTimestamp
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.DisposableBean
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.util.*
import java.util.function.Function
import java.util.regex.Pattern
import kotlin.test.assertFailsWith

/**
 * [SuspendKafkaConsumerTemplate]에 대한 테스트 클래스입니다.
 */
class SuspendKafkaConsumerTemplateTest: AbstractKafkaTest() {
    companion object: KLoggingChannel() {
        private val CONSUMER_GROUP = "$TEST_TOPIC_NAME-consumer-template-group"
    }

    private val receiver = mockk<KafkaReceiver<String, String>>()
    private val consumer = mockk<Consumer<String, String>>(relaxUnitFun = true)
    private val closableReceiver = mockk<KafkaReceiver<String, String>>(
        relaxUnitFun = true,
        moreInterfaces = arrayOf(AutoCloseable::class, DisposableBean::class)
    )

    @BeforeEach
    fun setupMocks() {
        clearMocks(receiver, consumer, closableReceiver)
    }

    @Test
    fun `ConsumerTemplate 생성`() {
        val receiverOptions =
            ReceiverOptions
                .create<String, String>(
                    mapOf(
                        "bootstrap.servers" to KafkaServer.Launcher.kafka.bootstrapServers,
                        "group.id" to CONSUMER_GROUP,
                        "key.deserializer" to org.apache.kafka.common.serialization.StringDeserializer::class.java,
                        "value.deserializer" to org.apache.kafka.common.serialization.StringDeserializer::class.java,
                        "auto.offset.reset" to "earliest",
                    ),
                ).subscription(listOf(TEST_TOPIC_NAME))

        val template = SuspendKafkaConsumerTemplate(receiverOptions)
        template.shouldNotBeNull()
    }

    @Test
    fun `구독과 구독 해제를 관리한다`() = runTest {
        val subscription = linkedSetOf<String>()

        every { consumer.subscribe(any<List<String>>()) } answers {
            subscription.clear()
            subscription.addAll(firstArg())
        }
        every { consumer.subscription() } answers { subscription.toSet() }
        every { consumer.unsubscribe() } answers { subscription.clear() }
        stubDoOnConsumer(receiver, consumer)

        val template = SuspendKafkaConsumerTemplate(receiver)

        template.subscribe("topic-a", "topic-b")
        template.subscription() shouldBeEqualTo setOf("topic-a", "topic-b")

        template.unsubscribe()
        template.subscription() shouldBeEqualTo emptySet()

        verify(exactly = 1) { consumer.subscribe(listOf("topic-a", "topic-b")) }
        verify(exactly = 1) { consumer.unsubscribe() }
    }

    @Test
    fun `정규식 기반 구독을 지원한다`() = runTest {
        val patternSlot = slot<Pattern>()

        every { consumer.subscribe(capture(patternSlot)) } returns Unit
        stubDoOnConsumer(receiver, consumer)

        val template = SuspendKafkaConsumerTemplate(receiver)
        template.subscribe(Pattern.compile("orders-.*"))

        patternSlot.captured.pattern() shouldBeEqualTo "orders-.*"
    }

    @Test
    fun `빈 topic 목록은 구독을 허용하지 않는다`() = runTest {
        stubDoOnConsumer(receiver, consumer)
        val template = SuspendKafkaConsumerTemplate(receiver)

        assertFailsWith<IllegalArgumentException> {
            template.subscribe()
        }
        assertFailsWith<IllegalArgumentException> {
            template.subscribe(" ")
        }
    }

    @Test
    fun `파티션 할당과 현재 위치 기준 커밋을 지원한다`() = runTest {
        val partition0 = TopicPartition(TEST_TOPIC_NAME, 0)
        val partition1 = TopicPartition(TEST_TOPIC_NAME, 1)
        val assignment = linkedSetOf(partition0, partition1)
        val commitSlot = slot<Map<TopicPartition, OffsetAndMetadata>>()

        every { consumer.assign(any<List<TopicPartition>>()) } answers { Unit }
        every { consumer.assignment() } returns assignment
        every { consumer.position(partition0) } returns 11L
        every { consumer.position(partition1) } returns 29L
        every { consumer.commitSync(capture(commitSlot)) } answers { Unit }
        stubDoOnConsumer(receiver, consumer)

        val template = SuspendKafkaConsumerTemplate(receiver)

        template.assign(partition0, partition1)
        val committed = template.commitCurrentOffsets()

        committed[partition0]?.offset() shouldBeEqualTo 11L
        committed[partition1]?.offset() shouldBeEqualTo 29L
        commitSlot.captured[partition0]?.offset() shouldBeEqualTo 11L
        commitSlot.captured[partition1]?.offset() shouldBeEqualTo 29L
    }

    @Test
    fun `timestamp 기준 seek 를 지원한다`() = runTest {
        val partition = TopicPartition(TEST_TOPIC_NAME, 0)
        val timestamp = 1_700_000_000_000L

        every { consumer.offsetsForTimes(mapOf(partition to timestamp)) } returns
            mapOf(partition to OffsetAndTimestamp(42L, timestamp, Optional.empty()))
        every { consumer.seek(partition, 42L) } answers { Unit }
        stubDoOnConsumer(receiver, consumer)

        val template = SuspendKafkaConsumerTemplate(receiver)
        val offset = template.seekToTimestamp(partition, timestamp)

        offset shouldBeEqualTo 42L
        verify(exactly = 1) { consumer.seek(partition, 42L) }
    }

    @Test
    fun `할당되지 않은 파티션은 현재 위치 커밋을 허용하지 않는다`() = runTest {
        val assigned = TopicPartition(TEST_TOPIC_NAME, 0)
        val unassigned = TopicPartition(TEST_TOPIC_NAME, 1)

        every { consumer.assignment() } returns setOf(assigned)
        stubDoOnConsumer(receiver, consumer)

        val template = SuspendKafkaConsumerTemplate(receiver)

        val error = assertFailsWith<IllegalArgumentException> {
            template.commitCurrentOffsets(unassigned)
        }

        error.message.shouldNotBeNull() shouldContain "unassigned partitions"
    }

    @Test
    fun `템플릿 종료 시 내부 CoroutineScope 를 취소한다`() = runTest {
        stubDoOnConsumer(closableReceiver, consumer)
        val template = SuspendKafkaConsumerTemplate(closableReceiver)
        val blocker = CompletableDeferred<Unit>()
        lateinit var launchedJob: Job

        launchedJob = template.launch {
            blocker.await()
        }

        template.close()
        launchedJob.cancelAndJoin()

        (template.coroutineContext[Job]?.isCancelled ?: false).shouldBeTrue()
        verify(exactly = 1) { (closableReceiver as AutoCloseable).close() }
    }

    private fun stubDoOnConsumer(
        receiver: KafkaReceiver<String, String>,
        consumer: Consumer<String, String>,
    ) {
        every { receiver.doOnConsumer<Any?>(any()) } answers {
            val callback = firstArg<Function<Consumer<String, String>, Any?>>()
            Mono.justOrEmpty(callback.apply(consumer))
        }
    }
}
