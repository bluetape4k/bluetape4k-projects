package io.bluetape4k.kafka.streams.kstream

import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Branched
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Grouped
import org.apache.kafka.streams.kstream.Joined
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.Repartitioned
import org.apache.kafka.streams.kstream.StreamJoined
import org.apache.kafka.streams.kstream.TableJoined
import org.apache.kafka.streams.state.KeyValueStore
import org.junit.jupiter.api.Test

/**
 * Kafka Streams KStream DSL 관련 유틸리티 함수에 대한 테스트 클래스입니다.
 */
class KStreamDslTest: AbstractKafkaTest() {
    companion object: KLoggingChannel()

    @Test
    fun `consumedOf로 Consumed 인스턴스 생성`() {
        val keySerde = Serdes.String()
        val valueSerde = Serdes.String()

        val consumed: Consumed<String, String> =
            consumedOf(
                keySerde = keySerde,
                valueSerde = valueSerde,
                resetPolicy = Topology.AutoOffsetReset.EARLIEST,
            )

        consumed.shouldNotBeNull()
    }

    @Test
    fun `producedOf로 Produced 인스턴스 생성`() {
        val keySerde = Serdes.String()
        val valueSerde = Serdes.String()

        val produced: Produced<String, String> =
            producedOf(
                keySerde = keySerde,
                valueSerde = valueSerde,
            )

        produced.shouldNotBeNull()
    }

    @Test
    fun `producedOf with processor name`() {
        val produced: Produced<String, String> = producedOf<String, String>("output-processor")

        produced.shouldNotBeNull()
    }

    @Test
    fun `joinedOf로 Joined 인스턴스 생성`() {
        val keySerde = Serdes.String()
        val valueSerde = Serdes.String()
        val otherValueSerde = Serdes.Long()

        val joined: Joined<String, String, Long> =
            joinedOf(
                keySerde = keySerde,
                valueSerde = valueSerde,
                otherValueSerde = otherValueSerde,
                name = "stream-join",
            )

        joined.shouldNotBeNull()
    }

    @Test
    fun `joinedOf with name only`() {
        val joined: Joined<String, String, Long> = joinedOf<String, String, Long>("join-name")

        joined.shouldNotBeNull()
    }

    @Test
    fun `groupedOf로 Grouped 인스턴스 생성`() {
        val keySerde = Serdes.String()
        val valueSerde = Serdes.Long()

        val grouped: Grouped<String, Long> =
            groupedOf(
                keySerde = keySerde,
                valueSerde = valueSerde,
                name = "group-by-key",
            )

        grouped.shouldNotBeNull()
    }

    @Test
    fun `groupedOf with processor name`() {
        val grouped: Grouped<String, String> = groupedOf<String, String>("group-processor")

        grouped.shouldNotBeNull()
    }

    @Test
    fun `materializedOf with store name`() {
        val materialized: Materialized<String, Long, KeyValueStore<org.apache.kafka.common.utils.Bytes, ByteArray>> =
            materializedOf("count-store")

        materialized.shouldNotBeNull()
    }

    @Test
    fun `materializedOf with serdes`() {
        val keySerde = Serdes.String()
        val valueSerde = Serdes.Long()

        val materialized: Materialized<String, Long, KeyValueStore<org.apache.kafka.common.utils.Bytes, ByteArray>> =
            materializedOf(keySerde, valueSerde)

        materialized.shouldNotBeNull()
    }

    @Test
    fun `streamJoinedOf with name`() {
        val streamJoined: StreamJoined<String, String, Long> = streamJoinedOf<String, String, Long>("stream-join-store")

        streamJoined.shouldNotBeNull()
    }

    @Test
    fun `streamJoinedOf with serdes`() {
        val keySerde = Serdes.String()
        val valueSerde = Serdes.String()
        val otherValueSerde = Serdes.Long()

        val streamJoined: StreamJoined<String, String, Long> =
            streamJoinedOf(
                keySerde = keySerde,
                valueSerde = valueSerde,
                otherValueSerde = otherValueSerde,
            )

        streamJoined.shouldNotBeNull()
    }

    @Test
    fun `repartitionedOf with name`() {
        val repartitioned: Repartitioned<String, String> = repartitionedOf<String, String>("repartition-step")

        repartitioned.shouldNotBeNull()
    }

    @Test
    fun `repartitionedOf with serdes`() {
        val keySerde = Serdes.String()
        val valueSerde = Serdes.Long()

        val repartitioned: Repartitioned<String, Long> =
            repartitionedOf(
                keySerde = keySerde,
                valueSerde = valueSerde,
            )

        repartitioned.shouldNotBeNull()
    }

    @Test
    fun `repartitionedOf with partition count`() {
        val repartitioned: Repartitioned<String, String> = repartitionedOf<String, String>(6)

        repartitioned.shouldNotBeNull()
    }

    @Test
    fun `tableJoinedOf with name`() {
        val tableJoined: TableJoined<String, Int> = tableJoinedOf<String, Int>("table-join")

        tableJoined.shouldNotBeNull()
    }

    @Test
    fun `branchedOf with name`() {
        val branched: Branched<String, String> = branchedOf<String, String>("valid-branch")

        branched.shouldNotBeNull()
    }

    @Test
    fun `branchedOf with function`() {
        val filterFunction: (KStream<String, String>) -> KStream<String, String> = { stream ->
            stream.filter { _, value -> value.startsWith("A") }
        }

        val branched: Branched<String, String> =
            branchedOf(
                chain = filterFunction,
                name = "starts-with-a",
            )

        branched.shouldNotBeNull()
    }

    @Test
    fun `branchedOf with consumer`() {
        val consumerFunction: (KStream<String, String>) -> Unit = { _ -> }

        val branched: Branched<String, String> =
            branchedOf(
                chain = consumerFunction,
                name = "consumer-branch",
            )

        branched.shouldNotBeNull()
    }
}
