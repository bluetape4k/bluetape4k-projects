package io.bluetape4k.redis.redisson.benchmark

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.redis.redisson.codec.FastForyCodec
import io.bluetape4k.redis.redisson.codec.ForyCodec
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import org.redisson.client.codec.Codec
import java.security.MessageDigest

private const val PREFLIGHT_PREFIX_SIZE = 3

/** Verifies Redisson direct-view and copied-composite benchmark semantics before JMH execution. */
object Issue756ForyRedissonBenchmarkPreflight {

    @JvmStatic
    fun main(args: Array<String>) {
        val cells = buildList {
            addAll(verifyBackend("fory", BinarySerializers.Fory, ForyCodec()))
            addAll(verifyBackend("fastFory", BinarySerializers.FastFory, FastForyCodec()))
        }
        check(cells.size == 12)
        println(
            """{"schema_version":1,"status":"passed","encode_disposition":"${encodeDisposition(args)}","methods":[${
                cells.joinToString(",") { it.toJson() }
            }]}""",
        )
    }
}

private data class RedissonPreflightCell(
    val method: String,
    val backend: String,
    val source: String,
    val path: String,
    val mode: String,
    val promotable: Boolean,
    val wireSha256: String,
) {
    fun toJson(): String =
        """{"method":"$method","backend":"$backend","source":"$source","path":"$path","mode":"$mode","promotable":$promotable,"wire_sha256":"$wireSha256","state_preserved":true}"""
}

private fun verifyBackend(
    backend: String,
    serializer: BinarySerializer,
    codec: Codec,
): List<RedissonPreflightCell> {
    val wire = serializer.serialize(ISSUE756_REDISSON_PAYLOAD)
    return Issue756RedissonSourceKind.entries.flatMap { sourceKind ->
        val source = preflightSource(sourceKind, wire)
        try {
            val readerIndex = source.readerIndex()
            val refCnt = source.refCnt()
            val copied = ByteBufUtil.getBytes(source, readerIndex, source.readableBytes(), true)
            check(serializer.deserialize<Any>(copied) == ISSUE756_REDISSON_PAYLOAD)
            check(codec.valueDecoder.decode(source, null) == ISSUE756_REDISSON_PAYLOAD)
            check(source.readerIndex() == readerIndex && source.refCnt() == refCnt)
            val sourceName = sourceKind.name.lowercase()
            val title = sourceName.replaceFirstChar(Char::uppercaseChar)
            val mode = if (sourceKind == Issue756RedissonSourceKind.COMPOSITE) "copied-fallback" else "direct-nio"
            val promotable = sourceKind != Issue756RedissonSourceKind.COMPOSITE
            listOf(
                RedissonPreflightCell(
                    method = "${backend}${title}DecodeCopiedBaseline",
                    backend = backend,
                    source = sourceName,
                    path = "baseline",
                    mode = "byte-array-baseline",
                    promotable = promotable,
                    wireSha256 = sha256(wire),
                ),
                RedissonPreflightCell(
                    method = "${backend}${title}DecodeCandidate",
                    backend = backend,
                    source = sourceName,
                    path = "candidate",
                    mode = mode,
                    promotable = promotable,
                    wireSha256 = sha256(wire),
                ),
            )
        } finally {
            source.release()
        }
    }
}

private fun preflightSource(kind: Issue756RedissonSourceKind, wire: ByteArray): ByteBuf =
    when (kind) {
        Issue756RedissonSourceKind.HEAP   -> PooledByteBufAllocator.DEFAULT.heapBuffer(PREFLIGHT_PREFIX_SIZE + wire.size)
            .writeZero(PREFLIGHT_PREFIX_SIZE)
            .writeBytes(wire)
            .readerIndex(PREFLIGHT_PREFIX_SIZE)

        Issue756RedissonSourceKind.DIRECT -> PooledByteBufAllocator.DEFAULT.directBuffer(PREFLIGHT_PREFIX_SIZE + wire.size)
            .writeZero(PREFLIGHT_PREFIX_SIZE)
            .writeBytes(wire)
            .readerIndex(PREFLIGHT_PREFIX_SIZE)

        Issue756RedissonSourceKind.COMPOSITE -> {
            val split = wire.size / 2
            Unpooled.compositeBuffer(2)
                .addComponents(
                    true,
                    Unpooled.buffer(PREFLIGHT_PREFIX_SIZE + split)
                        .writeZero(PREFLIGHT_PREFIX_SIZE)
                        .writeBytes(wire, 0, split),
                    Unpooled.wrappedBuffer(wire, split, wire.size - split),
                )
                .readerIndex(PREFLIGHT_PREFIX_SIZE)
        }
    }

private fun encodeDisposition(args: Array<String>): String =
    args.singleOrNull()?.also {
        check(it == "implemented" || it == "rejected") { "encode disposition must be implemented or rejected." }
    } ?: "rejected"

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
