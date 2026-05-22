package io.bluetape4k.kafka.codec

import io.bluetape4k.annotations.BluetapeDelicateApi
import org.junit.jupiter.api.Nested

class KafkaCodecTest {

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
    inner class Lz4KryoKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.Lz4Kryo
    }

    @Nested
    @OptIn(BluetapeDelicateApi::class)
    inner class Lz4ForyKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.Lz4Fory
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
    inner class ZstdKryoKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.ZstdKryo
    }

    @Nested
    @OptIn(BluetapeDelicateApi::class)
    inner class ZstdForyKafkaCodecTest: AbstractKafkaCodecTest() {
        override val codec: KafkaCodec<Any?> = KafkaCodecs.ZstdFory
    }
}
