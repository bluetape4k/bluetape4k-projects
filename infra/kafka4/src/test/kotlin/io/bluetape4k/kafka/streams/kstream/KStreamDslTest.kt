package io.bluetape4k.kafka.streams.kstream

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.mockk
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
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
import org.apache.kafka.streams.kstream.Window
import org.apache.kafka.streams.kstream.Windowed
import org.apache.kafka.streams.processor.StreamPartitioner
import org.apache.kafka.streams.processor.WallclockTimestampExtractor
import java.util.Optional
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.SessionStore
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.state.WindowStore
import org.junit.jupiter.api.Test
import java.time.Duration

class KStreamDslTest: AbstractKafkaTest() {
    companion object: KLoggingChannel()

    @Test
    fun `consumedOf로 Consumed 인스턴스 생성`() {
        val consumed: Consumed<String, String> =
            consumedOf(
                keySerde = Serdes.String(),
                valueSerde = Serdes.String(),
                resetPolicy = Topology.AutoOffsetReset.EARLIEST,
            )
        consumed.shouldNotBeNull()
    }

    @Test
    fun `consumedOf with timestamp extractor`() {
        val consumed: Consumed<String, String> =
            consumedOf(
                keySerde = Serdes.String(),
                valueSerde = Serdes.String(),
                timestampExtractor = WallclockTimestampExtractor(),
            )
        consumed.shouldNotBeNull()
    }

    @Test
    fun `producedOf로 Produced 인스턴스 생성`() {
        val produced: Produced<String, String> =
            producedOf(
                keySerde = Serdes.String(),
                valueSerde = Serdes.String(),
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
        val joined: Joined<String, String, Long> =
            joinedOf(
                keySerde = Serdes.String(),
                valueSerde = Serdes.String(),
                otherValueSerde = Serdes.Long(),
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
        val grouped: Grouped<String, Long> =
            groupedOf(
                keySerde = Serdes.String(),
                valueSerde = Serdes.Long(),
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
        val materialized: Materialized<String, Long, KeyValueStore<Bytes, ByteArray>> =
            materializedOf("count-store")
        materialized.shouldNotBeNull()
    }

    @Test
    fun `materializedOf with serdes`() {
        val materialized: Materialized<String, Long, KeyValueStore<Bytes, ByteArray>> =
            materializedOf(Serdes.String(), Serdes.Long())
        materialized.shouldNotBeNull()
    }

    @Test
    fun `materializedOf with store type`() {
        val materialized: Materialized<String, String, KeyValueStore<Bytes, ByteArray>> =
            materializedOf(Materialized.StoreType.IN_MEMORY)
        materialized.shouldNotBeNull()
    }

    @Test
    fun `materializedOf with KeyValueBytesStoreSupplier`() {
        val supplier = Stores.persistentKeyValueStore("kv-store")
        val materialized: Materialized<String, Long, KeyValueStore<Bytes, ByteArray>> =
            materializedOf(supplier)
        materialized.shouldNotBeNull()
    }

    @Test
    fun `materializedOf with WindowBytesStoreSupplier`() {
        val supplier = Stores.persistentWindowStore(
            "window-store",
            Duration.ofHours(1),
            Duration.ofMinutes(5),
            false,
        )
        val materialized: Materialized<String, Long, WindowStore<Bytes, ByteArray>> =
            materializedOf(supplier)
        materialized.shouldNotBeNull()
    }

    @Test
    fun `materializedOf with SessionBytesStoreSupplier`() {
        val supplier = Stores.persistentSessionStore("session-store", Duration.ofMinutes(30))
        val materialized: Materialized<String, Long, SessionStore<Bytes, ByteArray>> =
            materializedOf(supplier)
        materialized.shouldNotBeNull()
    }

    @Test
    fun `streamJoinedOf with name`() {
        val streamJoined: StreamJoined<String, String, Long> =
            streamJoinedOf<String, String, Long>("stream-join-store")
        streamJoined.shouldNotBeNull()
    }

    @Test
    fun `streamJoinedOf with serdes`() {
        val streamJoined: StreamJoined<String, String, Long> =
            streamJoinedOf(
                keySerde = Serdes.String(),
                valueSerde = Serdes.String(),
                otherValueSerde = Serdes.Long(),
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
        val repartitioned: Repartitioned<String, Long> =
            repartitionedOf(keySerde = Serdes.String(), valueSerde = Serdes.Long())
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
    fun `tableJoinedOf with partitioners`() {
        val leftPartitioner = StreamPartitioner<String, Void> { _, key, _, numPartitions ->
            Optional.of(setOf(Math.abs(key.hashCode()) % numPartitions))
        }
        val rightPartitioner = StreamPartitioner<Int, Void> { _, key, _, numPartitions ->
            Optional.of(setOf(Math.abs(key) % numPartitions))
        }
        val tableJoined: TableJoined<String, Int> = tableJoinedOf(leftPartitioner, rightPartitioner)
        tableJoined.shouldNotBeNull()
    }

    @Test
    fun `repartitionedOf with partitioner`() {
        val partitioner = StreamPartitioner<String, String> { _, key, _, numPartitions ->
            Optional.of(setOf(Math.abs(key.hashCode()) % numPartitions))
        }
        val repartitioned: Repartitioned<String, String> = repartitionedOf(partitioner)
        repartitioned.shouldNotBeNull()
    }

    @Test
    fun `streamJoinedOf with window store suppliers`() {
        val leftStore = Stores.persistentWindowStore(
            "left-join-store",
            java.time.Duration.ofMinutes(5),
            java.time.Duration.ofMinutes(1),
            true,
        )
        val rightStore = Stores.persistentWindowStore(
            "right-join-store",
            java.time.Duration.ofMinutes(5),
            java.time.Duration.ofMinutes(1),
            true,
        )
        val streamJoined: StreamJoined<String, String, Long> = streamJoinedOf(leftStore, rightStore)
        streamJoined.shouldNotBeNull()
    }

    @Test
    fun `windowedOf로 Windowed 인스턴스 생성`() {
        val window = mockk<Window>(relaxed = true)
        val windowed: Windowed<String> = windowedOf("user-123", window)

        windowed.shouldNotBeNull()
        windowed.key() shouldBeEqualTo "user-123"
        windowed.window() shouldBeEqualTo window
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
            branchedOf(chain = filterFunction, name = "starts-with-a")
        branched.shouldNotBeNull()
    }

    @Test
    fun `branchedOf with consumer`() {
        val consumerFunction: (KStream<String, String>) -> Unit = { _ -> }
        val branched: Branched<String, String> =
            branchedOf(chain = consumerFunction, name = "consumer-branch")
        branched.shouldNotBeNull()
    }
}
