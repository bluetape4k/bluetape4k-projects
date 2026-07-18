package io.bluetape4k.benchmark.serializer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeSameInstanceAs
import io.bluetape4k.io.serializer.BinarySerializer
import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.nio.ByteBuffer

class SerializerBenchmarkSupportTest {

    @Test
    fun `payload is deterministic and semantically comparable`() {
        val first = SerializerBenchmarkPayload.sample()
        val second = SerializerBenchmarkPayload.sample()

        first.semanticallyEquals(second).shouldBeTrue()
        first.payload shouldNotBeSameInstanceAs second.payload
    }

    @Test
    fun `binary compatibility adapter executes the interface default buffer path`() {
        val delegate = RecordingBinarySerializer()
        val adapter = CompatibilityBinarySerializer(delegate)
        val target = ByteBuffer.allocateDirect(256)

        adapter.serializeTo(SerializerBenchmarkPayload.sample(), target) shouldBeGreaterThan 0
        delegate.byteArraySerializeCalls shouldBeEqualTo 1
    }

    @Test
    fun `buffer validation rejects overflow and restores position`() {
        val fixture = binarySerializerBenchmarkFixture(BinarySerializerKind.JDK)
        val target = ByteBuffer.allocate(1)

        assertFailsWith<BufferOverflowException> {
            fixture.validateTarget(target)
        }
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `all production fixtures round trip without changing source state`() {
        serializerBenchmarkFixtures().forEach { fixture ->
            fixture.validate()
            val source = fixture.precomputedOptimizedSource()
            val position = source.position()
            val limit = source.limit()

            fixture.payload.semanticallyEquals(fixture.deserializeOptimized(source)).shouldBeTrue()
            source.position() shouldBeEqualTo position
            source.limit() shouldBeEqualTo limit
        }
    }

    @Test
    fun `fixture capability labels match production dispatch`() {
        val fixtures = serializerBenchmarkFixtures().associateBy { it.name }

        fixtures.getValue("jdk").claimEligibleSerialize.shouldBeTrue()
        fixtures.getValue("jdk").claimEligibleDeserialize.shouldBeTrue()
        fixtures.getValue("kryo").claimEligibleSerialize.shouldBeTrue()
        fixtures.getValue("kryo").claimEligibleDeserialize.shouldBeTrue()
        fixtures.getValue("fory").claimEligibleSerialize shouldBeEqualTo false
        fixtures.getValue("fory").claimEligibleDeserialize.shouldBeTrue()
        fixtures.getValue("fastjson2").claimEligibleSerialize shouldBeEqualTo false
        fixtures.getValue("fastjson2").claimEligibleDeserialize.shouldBeTrue()
    }
}

private class RecordingBinarySerializer: BinarySerializer {
    var byteArraySerializeCalls: Int = 0

    override fun serialize(graph: Any?): ByteArray {
        byteArraySerializeCalls++
        return byteArrayOf(1, 2, 3)
    }

    override fun <T: Any> deserialize(bytes: ByteArray?): T? = null
}
