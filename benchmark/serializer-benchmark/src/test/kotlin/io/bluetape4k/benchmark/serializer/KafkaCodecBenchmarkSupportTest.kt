package io.bluetape4k.benchmark.serializer

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class KafkaCodecBenchmarkSupportTest {
    @Test
    fun `fixture validates equivalent Kafka codec paths`() {
        val fixture = KafkaCodecBenchmarkFixture()

        fixture.validate()

        (fixture.wireSize > 0) shouldBeEqualTo true
    }
}
