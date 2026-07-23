package io.bluetape4k.redis.redisson.benchmark

import io.bluetape4k.io.serializer.ForyBinarySerializer
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import java.io.OutputStream
import java.security.MessageDigest

private class PreflightByteBufOutputStream(
    private val target: ByteBuf,
): OutputStream() {
    override fun write(value: Int) {
        target.writeByte(value)
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        target.writeBytes(bytes, offset, length)
    }

    override fun flush() = Unit

    override fun close() = Unit
}

/** Fail-closed wire, count, capacity, and ownership preflight for the feasibility probe. */
object Issue756ForyEncodeFeasibilityPreflight {
    @JvmStatic
    fun main(args: Array<String>) {
        val checks = listOf(
            verify("fory", ForyBinarySerializer()),
            verify("fastFory", ForyBinarySerializer.fast()),
        )
        println(
            """{"schemaVersion":1,"status":"passed","payloadSha256":"${payloadSha()}","checks":[${checks.joinToString()}]}""",
        )
    }

    private fun verify(name: String, serializer: ForyBinarySerializer): String {
        val expected = serializer.serialize(ISSUE756_FORY_PAYLOAD)
        val candidate = Unpooled.buffer(ISSUE756_FORY_INITIAL_CAPACITY, Int.MAX_VALUE)
        val initialCapacity = candidate.capacity()
        val candidateClass = candidate.javaClass.name
        val written: Int
        val finalCapacity: Int
        try {
            written = serializer.serializeBinaryToStream(
                ISSUE756_FORY_PAYLOAD,
                PreflightByteBufOutputStream(candidate),
            )
            finalCapacity = candidate.capacity()
            check(written == candidate.readableBytes()) { "$name stream count mismatch." }
            check(expected.contentEquals(ByteBufUtil.getBytes(candidate))) { "$name wire mismatch." }
        } finally {
            check(candidate.release()) { "$name candidate was not released exactly once." }
        }
        check(candidate.refCnt() == 0) { "$name candidate ownership leak." }
        val growthCount = if (finalCapacity == initialCapacity) 0 else 1
        return """{"backend":"$name","wireSha256":"${sha256(expected)}","written":$written,"initialCapacity":$initialCapacity,"finalCapacity":$finalCapacity,"growthCount":$growthCount,"heap":true,"bufferClass":"$candidateClass","released":true}"""
    }

    private fun payloadSha(): String =
        sha256(
            "${ISSUE756_FORY_PAYLOAD.id}|${ISSUE756_FORY_PAYLOAD.name}|${ISSUE756_FORY_PAYLOAD.description}"
                .toByteArray(),
        )

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
