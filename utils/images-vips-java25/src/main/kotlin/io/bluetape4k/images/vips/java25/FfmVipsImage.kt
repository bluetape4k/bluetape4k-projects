package io.bluetape4k.images.vips.java25

import app.photofox.vipsffm.VImage
import app.photofox.vipsffm.VipsError
import io.bluetape4k.images.vips.VipsDecodeException
import io.bluetape4k.images.vips.VipsEncodeException
import io.bluetape4k.images.vips.VipsEncodeOptions
import io.bluetape4k.images.vips.VipsImage
import io.bluetape4k.images.vips.VipsImageFormat
import io.bluetape4k.images.vips.VipsOperationException
import io.bluetape4k.images.vips.java25.ops.resizeWithFfm
import io.bluetape4k.images.vips.java25.ops.thumbnailWithFfm
import io.bluetape4k.images.vips.java25.writer.FfmVipsJpegWriter
import io.bluetape4k.images.vips.java25.writer.FfmVipsPngWriter
import io.bluetape4k.images.vips.java25.writer.FfmVipsWebpWriter
import kotlinx.coroutines.CancellationException
import java.io.OutputStream
import java.lang.foreign.Arena
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * vips-ffm FFM 바인딩 기반 [VipsImage] 구현체.
 *
 * 각 인스턴스는 `Arena.ofShared()`로 생성된 메모리 영역을 소유합니다.
 * `use {}` 블록 또는 `close()`로 반드시 리소스를 해제하십시오.
 *
 * vips-ffm의 연산(resize/thumbnail/crop)은 인스턴스 메서드로, 결과 VImage는 소스와 동일한 arena를 공유합니다.
 * 따라서 연산 결과 이미지는 부모 이미지보다 오래 살아서는 안 됩니다.
 *
 * @param arena 이 이미지를 소유하는 Arena (ownsArena=true일 때만 close() 시 해제)
 * @param vipsImage 래핑할 vips-ffm VImage
 * @param ownsArena true(기본값)이면 close() 시 arena를 해제합니다. 연산 결과 이미지는 false.
 */
internal class FfmVipsImage(
    private val arena: Arena,
    private val vipsImage: VImage,
    private val ownsArena: Boolean = true,
) : VipsImage {

    private val closed = AtomicBoolean(false)

    // 불변 프로퍼티: 생성 시점에 한 번만 읽어 캐싱합니다.
    override val width: Int = try {
        vipsImage.width
    } catch (e: VipsError) {
        throw VipsDecodeException("Failed to read width", e)
    }
    override val height: Int = try {
        vipsImage.height
    } catch (e: VipsError) {
        throw VipsDecodeException("Failed to read height", e)
    }
    override val bands: Int = try {
        vipsImage.getInt("bands") ?: throw VipsDecodeException("Failed to read bands count")
    } catch (e: VipsDecodeException) {
        throw e
    } catch (e: VipsError) {
        throw VipsDecodeException("Failed to read bands count", e)
    }

    private fun checkOpen() {
        if (closed.get()) throw IllegalStateException("VipsImage has been closed")
    }

    override fun resize(width: Int, height: Int): VipsImage {
        checkOpen()
        return try {
            val resized = resizeWithFfm(arena, vipsImage, width, height, this.width, this.height)
            FfmVipsImage(arena, resized, ownsArena = false)
        } catch (e: VipsOperationException) {
            throw e
        } catch (e: VipsDecodeException) {
            throw VipsOperationException("Image resize failed", e)
        } catch (e: VipsError) {
            throw VipsOperationException("Image resize failed", e)
        }
    }

    override fun thumbnail(maxDimension: Int): VipsImage {
        checkOpen()
        return try {
            val thumb = thumbnailWithFfm(arena, vipsImage, maxDimension)
            FfmVipsImage(arena, thumb, ownsArena = false)
        } catch (e: VipsOperationException) {
            throw e
        } catch (e: VipsDecodeException) {
            throw VipsOperationException("Image thumbnail failed", e)
        } catch (e: VipsError) {
            throw VipsOperationException("Image thumbnail failed", e)
        }
    }

    override fun crop(left: Int, top: Int, width: Int, height: Int): VipsImage {
        checkOpen()
        return try {
            val cropped = vipsImage.extractArea(left, top, width, height)
            FfmVipsImage(arena, cropped, ownsArena = false)
        } catch (e: VipsOperationException) {
            throw e
        } catch (e: VipsDecodeException) {
            throw VipsOperationException("Image crop failed", e)
        } catch (e: VipsError) {
            throw VipsOperationException("Image crop failed", e)
        }
    }

    override fun toBytes(format: VipsImageFormat, options: VipsEncodeOptions): ByteArray {
        checkOpen()
        return try {
            when (format) {
                VipsImageFormat.JPEG -> FfmVipsJpegWriter.writeToBytes(vipsImage, options)
                VipsImageFormat.PNG  -> FfmVipsPngWriter.writeToBytes(vipsImage, options)
                VipsImageFormat.WEBP -> FfmVipsWebpWriter.writeToBytes(vipsImage, options)
                else                 -> throw VipsEncodeException("Unsupported format for encoding: $format")
            }
        } catch (e: VipsEncodeException) {
            throw e
        } catch (e: VipsError) {
            throw VipsEncodeException("Image encoding failed: $format", e)
        }
    }

    override fun writeTo(path: Path, format: VipsImageFormat, options: VipsEncodeOptions) {
        checkOpen()
        try {
            path.toFile().writeBytes(toBytes(format, options))
        } catch (e: CancellationException) {
            throw e
        } catch (e: VipsEncodeException) {
            throw e
        } catch (e: Exception) {
            throw VipsEncodeException("Failed to write image to path: ${path.toAbsolutePath()}", e)
        }
    }

    override fun writeTo(out: OutputStream, format: VipsImageFormat, options: VipsEncodeOptions) {
        checkOpen()
        try {
            out.write(toBytes(format, options))
        } catch (e: CancellationException) {
            throw e
        } catch (e: VipsEncodeException) {
            throw e
        } catch (e: Exception) {
            throw VipsEncodeException("Failed to write image to output stream", e)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true) && ownsArena) {
            arena.close()
        }
    }
}
