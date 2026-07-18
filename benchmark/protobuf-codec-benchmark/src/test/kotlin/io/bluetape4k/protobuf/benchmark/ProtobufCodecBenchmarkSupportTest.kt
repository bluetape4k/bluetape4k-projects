package io.bluetape4k.protobuf.benchmark

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ProtobufCodecBenchmarkSupportTest {
    @Test
    fun `fixture validates semantic equality and stable payload identity`() {
        ProtobufCodecBenchmarkFixture().validate()
    }

    @Test
    fun `one thousand invocation resets do not overflow or read stale bytes`() {
        val fixture = ProtobufCodecBenchmarkFixture()
        val wire = fixture.serializerEncodeByteArray()
        repeat(1_000) {
            fixture.resetInvocation()
            val heapWritten = fixture.serializerEncodeHeap()
            fixture.serializerHeapTargetBytes(heapWritten) shouldBeEqualTo wire

            val directWritten = fixture.serializerEncodeDirect()
            fixture.serializerDirectTargetBytes(directWritten) shouldBeEqualTo wire

            fixture.serializerDecodeHeap() shouldBeEqualTo fixture.payload
            fixture.serializerDecodeDirect() shouldBeEqualTo fixture.payload
            fixture.redissonDecodeContiguous() shouldBeEqualTo fixture.payload
        }
    }

    @Test
    fun `expected benchmark matrix remains exact`() {
        ProtobufBenchmarkMatrix.expectedMethods.size shouldBeEqualTo 13
        ProtobufBenchmarkMatrix.claimEligible.size shouldBeEqualTo 5
    }

    @Test
    fun `metadata emits one canonical json line`() {
        val fixture = ProtobufCodecBenchmarkFixture().also { it.validate() }

        val output = captureStdout {
            ProtobufCodecBenchmarkMetadata.main(arrayOf("--json"))
        }

        val lines = output.lineSequence().filter { it.isNotBlank() }.toList()
        lines.size shouldBeEqualTo 1
        val metadata = ObjectMapper().readTree(lines.single())
        metadata.size() shouldBeEqualTo 8
        metadata["config_json"].textValue() shouldBeEqualTo fixture.configIdentity
        metadata["config_sha256"].textValue() shouldBeEqualTo fixture.configSha256
        metadata["matrix_version"].textValue() shouldBeEqualTo ProtobufBenchmarkMatrix.VERSION
        metadata["payload_sha256"].textValue() shouldBeEqualTo fixture.payloadSha256
        metadata["payload_size"].intValue() shouldBeEqualTo fixture.wireSize
        metadata["schema_version"].intValue() shouldBeEqualTo 1
        metadata["target_headroom"].intValue() shouldBeEqualTo ProtobufBenchmarkMatrix.TARGET_HEADROOM
        metadata["target_start"].intValue() shouldBeEqualTo ProtobufBenchmarkMatrix.TARGET_START
    }

    private fun captureStdout(block: () -> Unit): String {
        val original = System.out
        val output = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(output, true, Charsets.UTF_8))
            block()
        } finally {
            System.setOut(original)
        }
        return output.toString(Charsets.UTF_8)
    }
}
