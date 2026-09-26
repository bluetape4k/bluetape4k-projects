package io.bluetape4k.kafka.codec

import io.bluetape4k.annotations.BluetapeDelicateApi
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Nested

class KafkaCodecTest {

    companion object: KLogging()

    @Nested
    inner class JacksonCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = JacksonKafkaCodec(
            allowedTypePackages = setOf("io.bluetape4k.kafka.codec")
        )
    }

    @Nested
    inner class KryoKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.Kryo
    }

    @Nested
    @OptIn(BluetapeDelicateApi::class)
    inner class ForyKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.Fory
    }

    @Nested
    @OptIn(BluetapeDelicateApi::class)
    inner class FastForyKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.FastFory
    }

    @Nested
    inner class Lz4KryoKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.Lz4Kryo
    }

    @Nested
    @OptIn(BluetapeDelicateApi::class)
    inner class Lz4ForyKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.Lz4Fory
    }

    @Nested
    @OptIn(BluetapeDelicateApi::class)
    inner class Lz4FastForyKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.Lz4FastFory
    }

    @Nested
    inner class SnappyKryoKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.SnappyKryo
    }

    @Nested
    @OptIn(BluetapeDelicateApi::class)
    inner class SnappyForyKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.SnappyFory
    }

    @Nested
    @OptIn(BluetapeDelicateApi::class)
    inner class SnappyFastForyKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.SnappyFastFory
    }


    @Nested
    inner class ZstdKryoKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.ZstdKryo
    }

    @Nested
    @OptIn(BluetapeDelicateApi::class)
    inner class ZstdForyKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.ZstdFory
    }

    @Nested
    @OptIn(BluetapeDelicateApi::class)
    inner class ZstdFastForyKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.ZstdFastFory
    }
}
