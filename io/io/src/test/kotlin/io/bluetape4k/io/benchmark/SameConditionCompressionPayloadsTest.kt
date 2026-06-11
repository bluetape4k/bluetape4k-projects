package io.bluetape4k.io.benchmark

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class SameConditionCompressionPayloadsTest {

    @ParameterizedTest(name = "{0} {1} payload is deterministic and sized")
    @MethodSource("payloadCases")
    fun `same-condition payloads are deterministic and sized`(
        kind: SameConditionPayloadKind,
        size: SameConditionPayloadSize,
    ) {
        val first = SameConditionCompressionPayloads.payload(kind, size)
        val second = SameConditionCompressionPayloads.payload(kind, size)

        first shouldBeEqualTo second
        first.size shouldBeGreaterOrEqualTo size.targetBytes

        if (kind == SameConditionPayloadKind.Binary || kind == SameConditionPayloadKind.Random) {
            first.size shouldBeEqualTo size.targetBytes
        } else {
            first.size shouldBeLessThan size.targetBytes + 2048
        }
    }

    @ParameterizedTest(name = "{0} round-trips {1} {2}")
    @MethodSource("commonRoundTripCases")
    fun `common compressor matrix round-trips same-condition payloads`(
        compressorName: String,
        compressor: SameConditionCompressor,
        kind: SameConditionPayloadKind,
        size: SameConditionPayloadSize,
    ) {
        val payload = SameConditionCompressionPayloads.payload(kind, size)
        val compressed = compressor.compressor.compress(payload)

        compressed.shouldNotBeEmpty()
        compressor.compressor.decompress(compressed) shouldBeEqualTo payload
        compressor.name shouldBeEqualTo compressorName
    }

    @Test
    fun `bzip2 is kept as JVM-only context outside the normalized matrix`() {
        val payload = SameConditionCompressionPayloads.payload(
            SameConditionPayloadKind.Text,
            SameConditionPayloadSize.Medium,
        )
        val bzip2 = SameConditionCompressionPayloads.jvmContextCompressors.single()
        val compressed = bzip2.compressor.compress(payload)

        bzip2.name shouldBeEqualTo "bzip2"
        compressed.shouldNotBeEmpty()
        bzip2.compressor.decompress(compressed) shouldBeEqualTo payload
    }

    companion object {

        @JvmStatic
        fun payloadCases(): Stream<Arguments> =
            SameConditionPayloadKind.entries.flatMap { kind ->
                SameConditionPayloadSize.entries.map { size ->
                    Arguments.of(kind, size)
                }
            }.stream()

        @JvmStatic
        fun commonRoundTripCases(): Stream<Arguments> =
            SameConditionCompressionPayloads.commonCompressors.flatMap { compressor ->
                SameConditionPayloadKind.entries.flatMap { kind ->
                    SameConditionPayloadSize.entries.map { size ->
                        Arguments.of(compressor.name, compressor, kind, size)
                    }
                }
            }.stream()
    }
}
