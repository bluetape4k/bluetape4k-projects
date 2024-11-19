package io.bluetape4k.kafka.codec

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.RepeatedTest
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime

@RandomizedTest
abstract class AbstractKafkaCodecTest: AbstractKafkaTest() {

    companion object: KLogging()

    abstract val codec: KafkaCodec<Any?>

    @RepeatedTest(REPEAT_SIZE)
    fun `codec simple object`(@RandomValue data: MessageData) {
        val headers = RecordHeaders()
        val bytes = codec.serialize(TEST_TOPIC_NAME, headers, data)
        bytes.shouldNotBeEmpty()

        val actual = codec.deserialize(TEST_TOPIC_NAME, headers, bytes) as MessageData

        log.trace { "actual=$actual" }

        actual.shouldNotBeNull()
        actual shouldBeInstanceOf MessageData::class
        actual shouldBeEqualTo data
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `codec object graph`(@RandomValue master: MasterData) {
        master.details.shouldNotBeEmpty()

        val headers = RecordHeaders()
        val bytes = codec.serialize(TEST_TOPIC_NAME, headers, master)
        bytes.shouldNotBeEmpty()

        val actual = codec.deserialize(TEST_TOPIC_NAME, headers, bytes) as MasterData

        log.trace { "actual=$actual" }

        actual.shouldNotBeNull()
        actual.name shouldBeEqualTo master.name
        actual.createdAt shouldBeEqualTo master.createdAt
        actual.amount shouldBeEqualTo master.amount
    }


    data class MessageData(
        val name: String,
        val description: String?,
        val createdAt: LocalDateTime? = LocalDateTime.now(),
        val amount: BigDecimal? = null,
    ): Serializable

    data class MasterData(
        val name: String,
        val createdAt: Instant? = Instant.now(),
        val amount: BigDecimal? = null,
        val details: MutableList<DetailData> = mutableListOf(),
    ): Serializable

    data class DetailData(
        val name: String,
        val createdAt: OffsetDateTime? = OffsetDateTime.now(),
    ): Serializable
}
