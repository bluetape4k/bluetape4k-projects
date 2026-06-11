package io.bluetape4k.io.benchmark

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.SplittableRandom

enum class SameConditionPayloadKind {
    Json,
    Text,
    Binary,
    Random,
}

enum class SameConditionPayloadSize(val targetBytes: Int) {
    Small(1 * 1024),
    Medium(64 * 1024),
    Large(512 * 1024),
}

class SameConditionCompressor(
    val name: String,
    val compressor: Compressor,
) {
    override fun toString(): String = name
}

object SameConditionCompressionPayloads {

    val commonCompressors: List<SameConditionCompressor> = listOf(
        SameConditionCompressor("gzip", Compressors.GZip),
        SameConditionCompressor("deflate", Compressors.Deflate),
        SameConditionCompressor("zstd", Compressors.Zstd),
        SameConditionCompressor("lz4", Compressors.LZ4),
        SameConditionCompressor("snappy", Compressors.Snappy),
    )

    val jvmContextCompressors: List<SameConditionCompressor> = listOf(
        SameConditionCompressor("bzip2", Compressors.BZip2),
    )

    fun commonCompressor(name: String): SameConditionCompressor =
        commonCompressors.first { it.name == name }

    fun payload(kind: SameConditionPayloadKind, size: SameConditionPayloadSize): ByteArray =
        when (kind) {
            SameConditionPayloadKind.Json -> jsonPayload(size.targetBytes)
            SameConditionPayloadKind.Text -> textPayload(size.targetBytes)
            SameConditionPayloadKind.Binary -> binaryPayload(size.targetBytes)
            SameConditionPayloadKind.Random -> randomPayload(size.targetBytes)
        }

    private fun jsonPayload(targetBytes: Int): ByteArray {
        val out = StringBuilder(targetBytes + 512)
        out.append('[')

        var index = 0
        while (out.length < targetBytes) {
            if (index > 0) {
                out.append(',')
            }
            out.append(
                """{"id":$index,"tenant":"tenant-${index % 17}","service":"orders","region":"ap-northeast-${index % 3}","status":"${status(index)}","amount":${1000 + index % 7919},"tags":["blue","tape","compressor"],"message":"same condition json payload record $index"}"""
            )
            index++
        }

        out.append(']')
        return out.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun textPayload(targetBytes: Int): ByteArray =
        buildAsciiBytes(targetBytes) { index ->
            "2026-06-11T12:${(index % 60).toString().padStart(2, '0')}:00Z INFO service=orders " +
                    "trace=trace-${index % 8192} tenant=tenant-${index % 17} " +
                    "message=\"same condition compressor benchmark payload line $index\" " +
                    "path=/api/orders/${index % 512} latency_ms=${index % 200}\n"
        }

    private fun binaryPayload(targetBytes: Int): ByteArray =
        ByteArray(targetBytes) { index ->
            ((index * 31 + index / 7 + 0x5A) and 0xFF).toByte()
        }

    private fun randomPayload(targetBytes: Int): ByteArray {
        val random = SplittableRandom(0x4B1D_746L + targetBytes)
        return ByteArray(targetBytes) { random.nextInt(0, 256).toByte() }
    }

    private fun buildAsciiBytes(targetBytes: Int, lineFactory: (Int) -> String): ByteArray {
        val out = ByteArrayOutputStream(targetBytes + 512)
        var index = 0
        while (out.size() < targetBytes) {
            out.write(lineFactory(index).toByteArray(StandardCharsets.UTF_8))
            index++
        }
        return out.toByteArray()
    }

    private fun status(index: Int): String =
        when (index % 4) {
            0 -> "created"
            1 -> "paid"
            2 -> "shipped"
            else -> "settled"
        }
}
