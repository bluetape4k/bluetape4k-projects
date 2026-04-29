package io.bluetape4k.images.vips.java21

import com.criteo.vips.VipsImage
import com.criteo.vips.VipsException
import io.bluetape4k.images.vips.VipsDecodeException
import io.bluetape4k.images.vips.VipsImage as VipsImageApi
import io.bluetape4k.images.vips.VipsLimits
import io.bluetape4k.images.vips.java21.internal.NativeHandle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.input.BoundedInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Path

private val MAX_INPUT_BYTES = VipsLimits.MAX_INPUT_BYTES

// JPEG: FF D8 FF, PNG: 89 50 4E 47, WebP: 52 49 46 46 .. 57 45 42 50
private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())
private val WEBP_RIFF = byteArrayOf(0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte())
private val WEBP_MARKER = byteArrayOf(0x57.toByte(), 0x45.toByte(), 0x42.toByte(), 0x50.toByte())

/**
 * 바이트 배열에서 [VipsImageApi]를 생성합니다.
 *
 * 보안 검사 순서:
 * 1. 입력 크기 제한 — 최대 50 MB
 * 2. 포맷 허용 목록 (매직 바이트 검사) — JPEG, PNG, WebP만 허용
 * 3. maxPixels 초과 검사 (`width × height × bands`)
 *
 * @throws VipsDecodeException 지원하지 않는 포맷, 손상된 입력, 50 MB 초과, maxPixels 초과 시
 */
fun vipsImageOf(bytes: ByteArray): VipsImageApi {
    if (bytes.size.toLong() > MAX_INPUT_BYTES) {
        throw VipsDecodeException("Input bytes exceed ${MAX_INPUT_BYTES / (1024 * 1024)} MB limit")
    }
    checkFormatAllowlist(bytes)
    return decodeAndCheckPixels(bytes)
}

/**
 * [File]에서 [VipsImageApi]를 생성합니다.
 *
 * **경로 탐색(Path Traversal) 주의**: 호출자는 파일 경로가 허용된 디렉토리 내에 있음을 사전에 검증해야 합니다.
 *
 * @throws VipsDecodeException 지원하지 않는 포맷, 파일 읽기 실패, maxPixels 초과 시
 */
fun vipsImageOf(file: File): VipsImageApi =
    vipsImageOf(file.toPath())

/**
 * [Path]에서 [VipsImageApi]를 생성합니다.
 *
 * **경로 탐색(Path Traversal) 주의**: 호출자는 경로가 허용된 디렉토리 내에 있음을 사전에 검증해야 합니다.
 *
 * @throws VipsDecodeException 지원하지 않는 포맷, 파일 읽기 실패, maxPixels 초과 시
 */
fun vipsImageOf(path: Path): VipsImageApi {
    val header = path.toFile().inputStream().use { it.readNBytes(12) }
    checkFormatAllowlist(header)
    val size = path.toFile().length()
    if (size > MAX_INPUT_BYTES) {
        throw VipsDecodeException("File exceeds ${MAX_INPUT_BYTES / (1024 * 1024)} MB limit")
    }
    return decodeAndCheckPixels(path.toFile().readBytes())
}

/**
 * [InputStream]에서 [VipsImageApi]를 생성합니다.
 *
 * 입력 스트림은 최대 50 MB로 제한됩니다. 초과 시 [VipsDecodeException]이 발생합니다.
 *
 * @throws VipsDecodeException 포맷 미지원, 50 MB 초과, maxPixels 초과 시
 */
fun vipsImageOf(stream: InputStream): VipsImageApi {
    val bytes = readBounded(stream)
    checkFormatAllowlist(bytes)
    return decodeAndCheckPixels(bytes)
}

/**
 * [ByteArray]에서 [VipsImageApi]를 코루틴으로 생성합니다.
 *
 * 블로킹 JNI 연산을 [Dispatchers.IO]에서 실행합니다.
 */
suspend fun suspendVipsImageOf(bytes: ByteArray): VipsImageApi =
    withContext(Dispatchers.IO) { vipsImageOf(bytes) }

/**
 * [File]에서 [VipsImageApi]를 코루틴으로 생성합니다.
 *
 * **경로 탐색 주의**: 호출자가 경로를 사전 검증해야 합니다.
 */
suspend fun suspendVipsImageOf(file: File): VipsImageApi =
    withContext(Dispatchers.IO) { vipsImageOf(file) }

/**
 * [Path]에서 [VipsImageApi]를 코루틴으로 생성합니다.
 *
 * **경로 탐색 주의**: 호출자가 경로를 사전 검증해야 합니다.
 */
suspend fun suspendVipsImageOf(path: Path): VipsImageApi =
    withContext(Dispatchers.IO) { vipsImageOf(path) }

// ─── internal helpers ───────────────────────────────────────────────────────

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

private fun decodeAndCheckPixels(bytes: ByteArray): VipsImageApi {
    val nativeImage: VipsImage = try {
        VipsImage(bytes, bytes.size)
    } catch (e: VipsException) {
        throw VipsDecodeException("Image decode failed: unsupported format or corrupted input", e)
    }
    // NativeHandle 등록 전 픽셀 검사: 초과 시 nativeImage 해제 후 예외 (Cleaner 없음)
    val pixelCount = nativeImage.width.toLong() * nativeImage.height.toLong() * nativeImage.bands.toLong()
    val maxPixels = JVipsRuntime.maxPixels
    if (pixelCount < 0 || pixelCount > maxPixels) {
        nativeImage.release()
        throw VipsDecodeException(
            "Image exceeds maximum pixel count: $pixelCount > $maxPixels " +
                "(width=${nativeImage.width}, height=${nativeImage.height}, bands=${nativeImage.bands})"
        )
    }
    var handle: NativeHandle? = null
    return try {
        handle = NativeHandle(nativeImage)
        JVipsImage(handle)
    } catch (e: CancellationException) {
        handle?.close() ?: nativeImage.release()
        throw e
    } catch (e: Exception) {
        handle?.close() ?: nativeImage.release()
        when (e) {
            is VipsDecodeException -> throw e
            else -> throw VipsDecodeException("Image validation failed", e)
        }
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
