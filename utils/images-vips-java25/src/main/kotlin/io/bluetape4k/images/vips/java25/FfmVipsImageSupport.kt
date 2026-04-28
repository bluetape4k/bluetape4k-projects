package io.bluetape4k.images.vips.java25

import app.photofox.vipsffm.VImage
import app.photofox.vipsffm.VipsError
import io.bluetape4k.images.vips.VipsDecodeException
import io.bluetape4k.images.vips.VipsImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.input.BoundedInputStream
import java.io.File
import java.io.InputStream
import java.lang.foreign.Arena
import java.nio.file.Path

private const val MAX_INPUT_BYTES = 50L * 1024 * 1024  // 50 MB

private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())
private val WEBP_RIFF = byteArrayOf(0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte())
private val WEBP_MARKER = byteArrayOf(0x57.toByte(), 0x45.toByte(), 0x42.toByte(), 0x50.toByte())

/**
 * 바이트 배열에서 [VipsImage]를 생성합니다.
 *
 * 보안 검사 순서:
 * 1. 포맷 허용 목록 (매직 바이트) — JPEG, PNG, WebP만 허용
 * 2. maxPixels 초과 검사 (`width × height × bands`)
 *
 * @throws VipsDecodeException 지원하지 않는 포맷, 손상된 입력, maxPixels 초과 시
 */
fun ffmVipsImageOf(bytes: ByteArray): VipsImage {
    checkFormatAllowlist(bytes)
    return decodeAndCheckPixels(bytes)
}

/**
 * [File]에서 [VipsImage]를 생성합니다.
 *
 * **경로 탐색(Path Traversal) 주의**: 호출자는 파일 경로가 허용된 디렉토리 내에 있음을 사전에 검증해야 합니다.
 */
fun ffmVipsImageOf(file: File): VipsImage = ffmVipsImageOf(file.toPath())

/**
 * [Path]에서 [VipsImage]를 생성합니다.
 *
 * **경로 탐색(Path Traversal) 주의**: 호출자는 경로가 허용된 디렉토리 내에 있음을 사전에 검증해야 합니다.
 */
fun ffmVipsImageOf(path: Path): VipsImage {
    val header = path.toFile().inputStream().use { it.readNBytes(12) }
    checkFormatAllowlist(header)
    val arena = Arena.ofShared()
    return try {
        val vImage = VImage.newFromFile(arena, path.toAbsolutePath().toString())
        checkPixelCount(vImage, arena)
        FfmVipsImage(arena, vImage)
    } catch (e: VipsDecodeException) {
        arena.close()
        throw e
    } catch (e: VipsError) {
        arena.close()
        throw VipsDecodeException("Image decode failed", e)
    }
}

/**
 * [InputStream]에서 [VipsImage]를 생성합니다.
 *
 * 입력 스트림은 최대 50 MB로 제한됩니다. 초과 시 [VipsDecodeException]이 발생합니다.
 */
fun ffmVipsImageOf(stream: InputStream): VipsImage {
    val bytes = readBounded(stream)
    checkFormatAllowlist(bytes)
    return decodeAndCheckPixels(bytes)
}

/** [ByteArray]에서 [VipsImage]를 코루틴으로 생성합니다. */
suspend fun suspendFfmVipsImageOf(bytes: ByteArray): VipsImage =
    withContext(Dispatchers.IO) { ffmVipsImageOf(bytes) }

/** [File]에서 [VipsImage]를 코루틴으로 생성합니다. */
suspend fun suspendFfmVipsImageOf(file: File): VipsImage =
    withContext(Dispatchers.IO) { ffmVipsImageOf(file) }

/** [Path]에서 [VipsImage]를 코루틴으로 생성합니다. */
suspend fun suspendFfmVipsImageOf(path: Path): VipsImage =
    withContext(Dispatchers.IO) { ffmVipsImageOf(path) }

// ─── internal helpers ────────────────────────────────────────────────────────

private fun readBounded(stream: InputStream): ByteArray {
    val bounded = BoundedInputStream.builder()
        .setInputStream(stream)
        .setMaxCount(MAX_INPUT_BYTES)
        .setPropagateClose(false)
        .setOnMaxCount { _, maxCount ->
            throw VipsDecodeException("Input stream exceeds ${maxCount / (1024 * 1024)} MB limit")
        }
        .get()
    return bounded.readBytes()
}

private fun checkFormatAllowlist(bytes: ByteArray) {
    if (bytes.startsWith(JPEG_MAGIC)) return
    if (bytes.startsWith(PNG_MAGIC)) return
    if (bytes.startsWith(WEBP_RIFF) && bytes.size >= 12 && bytes.regionMatches(8, WEBP_MARKER)) return
    throw VipsDecodeException("Unsupported image format — only JPEG, PNG, and WebP are allowed")
}

private fun decodeAndCheckPixels(bytes: ByteArray): VipsImage {
    val arena = Arena.ofShared()
    return try {
        val vImage = VImage.newFromBytes(arena, bytes)
        checkPixelCount(vImage, arena)
        FfmVipsImage(arena, vImage)
    } catch (e: VipsDecodeException) {
        arena.close()
        throw e
    } catch (e: VipsError) {
        arena.close()
        throw VipsDecodeException("Image decode failed: unsupported format or corrupted input", e)
    }
}

private fun checkPixelCount(vImage: VImage, arena: Arena) {
    val bands = vImage.getInt("bands") ?: 3
    val pixelCount = vImage.width.toLong() * vImage.height * bands
    val maxPixels = FfmVipsRuntime.maxPixels
    if (pixelCount > maxPixels) {
        arena.close()
        throw VipsDecodeException(
            "Image exceeds maximum pixel count: $pixelCount > $maxPixels " +
                "(width=${vImage.width}, height=${vImage.height}, bands=$bands)"
        )
    }
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (size < prefix.size) return false
    for (i in prefix.indices) {
        if (this[i] != prefix[i]) return false
    }
    return true
}

private fun ByteArray.regionMatches(offset: Int, other: ByteArray): Boolean {
    if (size < offset + other.size) return false
    for (i in other.indices) {
        if (this[offset + i] != other[i]) return false
    }
    return true
}
