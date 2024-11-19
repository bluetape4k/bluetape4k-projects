package io.bluetape4k.grpc.testing.integration

import com.google.protobuf.MessageLite
import io.grpc.Metadata
import io.grpc.Metadata.Key
import io.grpc.protobuf.ProtoUtils
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import java.net.URI
import java.net.URISyntaxException

object Util {

    @JvmField
    val METADATA_KEY: Key<Messages.SimpleContext> =
        Metadata.Key.of(
            "grpc.testing.SimpleContext${Metadata.BINARY_HEADER_SUFFIX}",
            ProtoUtils.metadataMarshaller(Messages.SimpleContext.getDefaultInstance())
        )

    @JvmField
    val ECHO_INITIAL_METADATA_KEY: Key<String> =
        Metadata.Key.of("x-grpc-test-echo-initial", Metadata.ASCII_STRING_MARSHALLER)

    @JvmField
    val ECHO_TRAILING_METADATA_KEY: Key<ByteArray> =
        Metadata.Key.of("x-grpc-test-echo-trailing-bin", Metadata.BINARY_BYTE_MARSHALLER)

    /**
     * Combine a host and port into an authority string.
     */
    fun authorityFromHostAndPort(host: String, port: Int): String {
        return try {
            URI(null, null, host, port, null, null, null).authority
        } catch (ex: URISyntaxException) {
            throw IllegalArgumentException("Invalid host or port: $host:$port", ex)
        }
    }

    /**
     * Assert that two messages are equal, producing a useful message if not.
     */
    fun assertEquals(expected: MessageLite?, actual: MessageLite?) {
        if (expected == null || actual == null) {
            actual shouldBeEqualTo expected
        } else {
            // This assertEquals should always complete.
            actual.toString() shouldBeEqualTo expected.toString()

            // But if it doesn't, then this should.
            actual shouldNotBeEqualTo expected
        }
    }

    fun assertEquals(expected: List<MessageLite>?, actual: List<MessageLite>?) {
        if (expected == null || actual == null) {
            actual shouldBeEqualTo expected
        } else if (expected.size == actual.size) {
            actual shouldBeEqualTo expected
        } else {
            actual.indices.forEach { i ->
                actual[i] shouldBeEqualTo expected[i]
            }
        }
    }
}
