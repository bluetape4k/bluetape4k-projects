package org.springframework.kafka.core

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.concurrent.onSuccess
import io.bluetape4k.kafka.spring.test.utils.consumerProps
import io.bluetape4k.kafka.spring.test.utils.getSingleRecord
import io.bluetape4k.kafka.spring.test.utils.producerProps
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.messaging.support.message
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.support.uninitialized
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.springframework.aop.framework.ProxyFactory
import org.springframework.kafka.core.KafkaTemplateTests.Companion.INT_KEY_TOPIC
import org.springframework.kafka.core.KafkaTemplateTests.Companion.STRING_KEY_TOPIC
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.CompositeProducerListener
import org.springframework.kafka.support.DefaultKafkaHeaderMapper
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.KafkaUtils
import org.springframework.kafka.support.ProducerListener
import org.springframework.kafka.support.SendResult
import org.springframework.kafka.support.TopicPartitionOffset
import org.springframework.kafka.support.converter.MessagingMessageConverter
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.condition.EmbeddedKafkaCondition
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.messaging.Message
import org.springframework.test.context.junit4.SpringRunner
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(SpringRunner::class)
@EmbeddedKafka(topics = [INT_KEY_TOPIC, STRING_KEY_TOPIC])
class KafkaTemplateTests {

    companion object: KLoggingChannel() {
        private const val INT_KEY_TOPIC = "intKeyTopic"
        private const val STRING_KEY_TOPIC = "stringKeyTopic"
    }

    private var embeddedKafka: EmbeddedKafkaBroker = uninitialized()
    private var consumer: Consumer<Int, String> = uninitialized()

    private val noopListener: ProducerFactory.Listener<String, String> =
        object: ProducerFactory.Listener<String, String> {
            override fun producerAdded(id: String, producer: Producer<String, String>) {}
            override fun producerRemoved(id: String, producer: Producer<String, String>) {}
        }

    private val noopProducerPostProcessor =
        ProducerPostProcessor { processor: Producer<String, String> -> processor }

    @BeforeAll
    fun setUp() {
        embeddedKafka = EmbeddedKafkaCondition.getBroker()
        val consumerProps = embeddedKafka.consumerProps("KafkaTemplatetests" + UUID.randomUUID(), false)
        val cf = DefaultKafkaConsumerFactory<Int, String>(consumerProps)
        consumer = cf.createConsumer()
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, INT_KEY_TOPIC)
    }

    @AfterAll
    fun tearDown() {
        consumer.close()
    }

    @Test
    fun `send message`() {
        val senderProps = embeddedKafka.producerProps()
        val pf = DefaultKafkaProducerFactory<Int, String>(senderProps)
        val wrapper = AtomicReference<Producer<Int, String>>(null)
        pf.addPostProcessor { producer ->
            val prox = ProxyFactory()
            prox.setTarget(producer)
            @Suppress("UNCHECKED_CAST")
            val proxy = prox.proxy as Producer<Int, String>
            wrapper.set(proxy)
            proxy
        }
        val template = KafkaTemplate(pf, true)
        template.defaultTopic = INT_KEY_TOPIC
        template.setConsumerFactory(
            DefaultKafkaConsumerFactory(
                embeddedKafka.consumerProps("xx", false)
            )
        )

        // val initialRecords = template.receive(listOf(TopicPartitionOffset(INT_KEY_TOPIC, 1, 1L)))
        // initialRecords.shouldBeEmpty()

        template.sendDefault("foo")
        consumer.getSingleRecord(INT_KEY_TOPIC).value() shouldBeEqualTo "foo"

        template.sendDefault(0, 2, "bar")
        var received: ConsumerRecord<Int, String> = consumer.getSingleRecord(INT_KEY_TOPIC)
        with(received) {
            partition() shouldBeEqualTo 0
            key() shouldBeEqualTo 2
            value() shouldBeEqualTo "bar"
        }

        template.sendDefault(1, 3, "qux")
        consumer.getSingleRecord(INT_KEY_TOPIC).apply {
            partition() shouldBeEqualTo 1
            key() shouldBeEqualTo 3
            value() shouldBeEqualTo "qux"
        }

        val message = message("fiz") {
            setHeader(KafkaHeaders.TOPIC, INT_KEY_TOPIC)
            setHeader(KafkaHeaders.PARTITION, 0)
            setHeader(KafkaHeaders.KEY, 2)
        }
        template.send(message)
        consumer.getSingleRecord(INT_KEY_TOPIC).apply {
            partition() shouldBeEqualTo 0
            key() shouldBeEqualTo 2
            value() shouldBeEqualTo "fiz"
        }

        val message2 = message("buz") {
            setHeader(KafkaHeaders.PARTITION, 1)
            setHeader(KafkaHeaders.KEY, 2)
        }
        template.send(message2)
        received = consumer.getSingleRecord(INT_KEY_TOPIC).apply {
            partition() shouldBeEqualTo 1
            key() shouldBeEqualTo 2
            value() shouldBeEqualTo "buz"
        }

        val metrics = template.execute { it.metrics() }
        metrics.shouldNotBeNull().shouldNotBeEmpty()
        val metrics2 = template.metrics()
        metrics2.shouldNotBeEmpty()

        val partitions = template.partitionsFor(INT_KEY_TOPIC)
        partitions.shouldNotBeEmpty() shouldHaveSize 2
        KafkaTestUtils.getPropertyValue(pf.createProducer(), "delegate") shouldBeEqualTo wrapper.get()

        // 마지막에 수신한 buz
        val receive = template.receive(INT_KEY_TOPIC, 1, received.offset())!!
        with(receive) {
            partition() shouldBeEqualTo 1
            key() shouldBeEqualTo 2
            value() shouldBeEqualTo "buz"
        }

        val records = template.receive(
            listOf(
                TopicPartitionOffset(INT_KEY_TOPIC, 1, 1L),
                TopicPartitionOffset(INT_KEY_TOPIC, 0, 1L),
                TopicPartitionOffset(INT_KEY_TOPIC, 0, 0L),
                TopicPartitionOffset(INT_KEY_TOPIC, 1, 0L),
            )
        )
        records.count() shouldBeEqualTo 4
        val partition2 = records.partitions()
        partition2 shouldContainSame setOf(TopicPartition(INT_KEY_TOPIC, 1), TopicPartition(INT_KEY_TOPIC, 0))

        records.records(TopicPartition(INT_KEY_TOPIC, 1)).map { it.offset() } shouldContainSame listOf(0L, 1L)
        records.records(TopicPartition(INT_KEY_TOPIC, 0)).map { it.offset() } shouldContainSame listOf(0L, 1L)

        pf.destroy()
    }

    @Test
    fun `send message with timestamp`() {
        val senderProps = embeddedKafka.producerProps()
        val pf = DefaultKafkaProducerFactory<Int, String>(senderProps)
        val template = KafkaTemplate(pf, true).apply {
            defaultTopic = INT_KEY_TOPIC
        }

        template.sendDefault(0, 1487694048607L, 0, "foo-ts1")
        consumer.getSingleRecord(INT_KEY_TOPIC).apply {
            partition() shouldBeEqualTo 0
            timestamp() shouldBeEqualTo 1487694048607L
            value() shouldBeEqualTo "foo-ts1"
        }

        template.send(INT_KEY_TOPIC, 1, 1487694048610L, 0, "foo-ts2")
        consumer.getSingleRecord(INT_KEY_TOPIC).apply {
            partition() shouldBeEqualTo 1
            timestamp() shouldBeEqualTo 1487694048610L
            value() shouldBeEqualTo "foo-ts2"
        }

        val metrics = template.execute { it.metrics() }
        metrics.shouldNotBeNull().shouldNotBeEmpty()

        val metrics2 = template.metrics()
        metrics2.shouldNotBeEmpty()

        val partitions = template.partitionsFor(INT_KEY_TOPIC)
        partitions.shouldNotBeEmpty() shouldHaveSize 2
        partitions.forEach {
            log.debug { "partition=$it" }
        }

        pf.destroy()
    }

    @Test
    fun `send with message`() {
        val senderProps = embeddedKafka.producerProps()
        val pf = DefaultKafkaProducerFactory<Int, String>(senderProps)
        val template = KafkaTemplate(pf, true)

        val message1 = message("foo-message") {
            setHeader(KafkaHeaders.TOPIC, INT_KEY_TOPIC)
            setHeader(KafkaHeaders.PARTITION, 0)
            setHeader("foo", "bar")
            setHeader(KafkaHeaders.RECEIVED_TOPIC, "dummy")
        }
        template.send(message1)

        val r1 = consumer.getSingleRecord(INT_KEY_TOPIC)
        r1.value() shouldBeEqualTo "foo-message"

        val iterator = r1.headers().iterator()

        iterator.hasNext().shouldBeTrue()
        var next = iterator.next()
        next.value().toUtf8String() shouldBeEqualTo "bar"

        iterator.hasNext().shouldBeTrue()
        next = iterator.next()
        next.key() shouldBeEqualTo DefaultKafkaHeaderMapper.JSON_TYPES

        r1.headers().forEach {
            log.debug { "Received message header. key=${it.key()}, value=${it.value().toUtf8String()}" }
        }

        val message2 = message("foo-message-2") {
            setHeader(KafkaHeaders.TOPIC, INT_KEY_TOPIC)
            setHeader(KafkaHeaders.PARTITION, 0)
            setHeader(KafkaHeaders.TIMESTAMP, 1487694048615L)
            setHeader("foo", "bar")
        }
        template.send(message2)

        val r2 = consumer.getSingleRecord(INT_KEY_TOPIC)
        r2.value() shouldBeEqualTo "foo-message-2"
        r2.timestamp() shouldBeEqualTo 1487694048615L

        val messageConverter = MessagingMessageConverter()

        val ack = mockk<Acknowledgment>(relaxUnitFun = true)
        val mockConsumer = mockk<Consumer<*, *>>(relaxUnitFun = true)
        KafkaUtils.setConsumerGroupId("test.group.id")
        val recordToMessage: Message<*> = messageConverter.toMessage(r2, ack, mockConsumer, String::class.java)

        with(recordToMessage.headers) {
            get(KafkaHeaders.TIMESTAMP_TYPE) shouldBeEqualTo "CREATE_TIME"
            get(KafkaHeaders.RECEIVED_TIMESTAMP) shouldBeEqualTo 1487694048615L
            get(KafkaHeaders.RECEIVED_TOPIC) shouldBeEqualTo INT_KEY_TOPIC
            get(KafkaHeaders.ACKNOWLEDGMENT) shouldBeEqualTo ack
            get("foo") shouldBeEqualTo "bar"
            get(KafkaHeaders.GROUP_ID) shouldBeEqualTo "test.group.id"
        }
        recordToMessage.payload shouldBeEqualTo "foo-message-2"

        KafkaUtils.clearConsumerGroupId()
        pf.destroy()
    }

    @Test
    fun `with producer listener`() {
        val senderProps = embeddedKafka.producerProps()
        val pf = DefaultKafkaProducerFactory<Int, String>(senderProps)
        val template = KafkaTemplate(pf).apply { defaultTopic = INT_KEY_TOPIC }
        val latch = CountDownLatch(2)
        val records = fastListOf<ProducerRecord<Int, String>>()
        val meta = fastListOf<RecordMetadata>()
        val onErrorDelegateCalls = AtomicInteger(0)

        class PL: ProducerListener<Int, String> {
            override fun onSuccess(producerRecord: ProducerRecord<Int, String>, recordMetadata: RecordMetadata) {
                records.add(producerRecord)
                meta.add(recordMetadata)
                latch.countDown()
            }

            override fun onError(
                producerRecord: ProducerRecord<Int, String>?,
                recordMetadata: RecordMetadata?,
                exception: Exception?,
            ) {
                producerRecord.shouldNotBeNull()
                exception.shouldNotBeNull()
                onErrorDelegateCalls.incrementAndGet()
            }
        }

        val pl1 = PL()
        val pl2 = PL()
        val cpl = CompositeProducerListener(pl1, pl2)
        template.setProducerListener(cpl)
        template.sendDefault("foo")
        template.flush()

        latch.await(10, TimeUnit.SECONDS).shouldBeTrue()
        records[0].value() shouldBeEqualTo "foo"
        records[1].value() shouldBeEqualTo "foo"
        meta[0].topic() shouldBeEqualTo INT_KEY_TOPIC
        meta[1].topic() shouldBeEqualTo INT_KEY_TOPIC

        consumer.getSingleRecord(INT_KEY_TOPIC)
        pf.destroy()
        cpl.onError(
            records[0],
            RecordMetadata(TopicPartition(INT_KEY_TOPIC, -1), 0L, 0, 0L, 0, 0),
            RuntimeException("x")
        )
        onErrorDelegateCalls.get() shouldBeEqualTo 2
    }

    @Test
    fun `with producer record listener`() {
        val senderProps = embeddedKafka.producerProps()
        val pf = DefaultKafkaProducerFactory<Int, String>(senderProps)
        val template = KafkaTemplate(pf).apply { defaultTopic = INT_KEY_TOPIC }
        val latch = CountDownLatch(1)
        template.setProducerListener(object: ProducerListener<Int, String> {
            override fun onSuccess(producerRecord: ProducerRecord<Int, String>?, recordMetadata: RecordMetadata?) {
                latch.countDown()
            }
        })

        template.sendDefault("foo")
        template.flush()
        latch.await(10, TimeUnit.SECONDS).shouldBeTrue()

        // Drain the topic
        consumer.getSingleRecord(INT_KEY_TOPIC)
        pf.destroy()
    }

    @Test
    fun `produce with callback`() {
        val senderProps = embeddedKafka.producerProps()
        val pf = DefaultKafkaProducerFactory<Int, String>(senderProps)
        val template = KafkaTemplate(pf, true).apply { defaultTopic = INT_KEY_TOPIC }

        val future = template.sendDefault("foo")
        template.flush()

        val latch = CountDownLatch(1)
        val theResult = AtomicReference<SendResult<Int, String>>(null)

        future.onSuccess { result ->
            theResult.set(result)
            latch.countDown()
        }

        consumer.getSingleRecord(INT_KEY_TOPIC).value() shouldBeEqualTo "foo"
        latch.await(5, TimeUnit.SECONDS).shouldBeTrue()
        pf.destroy()
    }
}
