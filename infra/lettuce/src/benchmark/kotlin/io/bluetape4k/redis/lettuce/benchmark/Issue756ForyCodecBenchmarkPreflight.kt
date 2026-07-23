package io.bluetape4k.redis.lettuce.benchmark

import io.bluetape4k.io.serializer.ForyBinarySerializer
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import java.security.MessageDigest

private val FORY_METHODS = listOf(
    "foryHeapCopiedBaseline",
    "foryHeapCandidate",
    "foryDirectCopiedBaseline",
    "foryDirectCandidate",
    "fastForyHeapCopiedBaseline",
    "fastForyHeapCandidate",
    "fastForyDirectCopiedBaseline",
    "fastForyDirectCandidate",
)

/** Verifies wire parity and caller-owned buffer state before canonical Fory measurements. */
object Issue756ForyCodecBenchmarkPreflight {

    @JvmStatic
    fun main(args: Array<String>) {
        val cells = buildList {
            for ((backend, serializer) in listOf(
                "fory" to ForyBinarySerializer(),
                "fastFory" to ForyBinarySerializer.fast(),
            )) {
                for ((targetName, direct) in listOf("heap" to false, "direct" to true)) {
                    addAll(verifyPair(backend, targetName, direct, serializer))
                }
            }
        }
        check(cells.map { it.method } == FORY_METHODS)
        val payloadSha = sha256(ForyBinarySerializer().serialize(ISSUE756_PAYLOAD))
        println(
            """{"schema_version":1,"status":"passed","payload_sha256":"$payloadSha","methods":[${
                cells.joinToString(",") { it.toJson() }
            }]}""",
        )
    }
}

private data class ForyPreflightCell(
    val method: String,
    val backend: String,
    val target: String,
    val path: String,
    val wireSha256: String,
    val writtenCount: Int,
) {
    fun toJson(): String =
        """{"method":"$method","backend":"$backend","target":"$target","path":"$path","wire_sha256":"$wireSha256","written_count":$writtenCount,"prefix_preserved":true,"state_preserved":true}"""
}

private fun verifyPair(
    backend: String,
    targetName: String,
    direct: Boolean,
    serializer: ForyBinarySerializer,
): List<ForyPreflightCell> {
    val expected = serializer.serialize(ISSUE756_PAYLOAD)
    val target = allocateTarget(direct)
    try {
        target.setIndex(ISSUE756_READER_INDEX, ISSUE756_START_INDEX)
        target.setByte(ISSUE756_PREFIX_INDEX, ISSUE756_PREFIX.toInt())
        val beforeCapacity = target.capacity()
        val beforeRefCnt = target.refCnt()
        val codec = LettuceBinaryCodec<Issue756BenchmarkData>(serializer)
        codec.encodeValue(ISSUE756_PAYLOAD, target)
        val actual = ByteArray(target.writerIndex() - ISSUE756_START_INDEX)
        target.getBytes(ISSUE756_START_INDEX, actual)
        check(actual.contentEquals(expected))
        check(target.readerIndex() == ISSUE756_READER_INDEX)
        check(target.capacity() == beforeCapacity && target.refCnt() == beforeRefCnt)
        check(target.getByte(ISSUE756_PREFIX_INDEX) == ISSUE756_PREFIX)
        val title = targetName.replaceFirstChar(Char::uppercaseChar)
        val prefix = if (backend == "fory") "fory" else "fastFory"
        return listOf(
            ForyPreflightCell(
                method = "${prefix}${title}CopiedBaseline",
                backend = backend,
                target = targetName,
                path = "baseline",
                wireSha256 = sha256(expected),
                writtenCount = expected.size,
            ),
            ForyPreflightCell(
                method = "${prefix}${title}Candidate",
                backend = backend,
                target = targetName,
                path = "candidate",
                wireSha256 = sha256(actual),
                writtenCount = actual.size,
            ),
        )
    } finally {
        target.release()
    }
}

private fun allocateTarget(direct: Boolean): ByteBuf =
    if (direct) {
        PooledByteBufAllocator.DEFAULT.directBuffer(ISSUE756_TARGET_CAPACITY, ISSUE756_TARGET_CAPACITY)
    } else {
        PooledByteBufAllocator.DEFAULT.heapBuffer(ISSUE756_TARGET_CAPACITY, ISSUE756_TARGET_CAPACITY)
    }

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
