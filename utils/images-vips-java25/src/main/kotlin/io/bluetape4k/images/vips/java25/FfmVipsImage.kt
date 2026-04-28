package io.bluetape4k.images.vips.java25

import app.photofox.vipsffm.VImage
import app.photofox.vipsffm.VipsError
import app.photofox.vipsffm.VipsOption
import io.bluetape4k.images.vips.VipsDecodeException
import io.bluetape4k.images.vips.VipsEncodeException
import io.bluetape4k.images.vips.VipsEncodeOptions
import io.bluetape4k.images.vips.VipsImage
import io.bluetape4k.images.vips.VipsImageFormat
import io.bluetape4k.images.vips.java25.ops.resizeWithFfm
import io.bluetape4k.images.vips.java25.ops.thumbnailWithFfm
import io.bluetape4k.images.vips.java25.writer.FfmVipsJpegWriter
import io.bluetape4k.images.vips.java25.writer.FfmVipsPngWriter
import io.bluetape4k.images.vips.java25.writer.FfmVipsWebpWriter
import java.io.OutputStream
import java.lang.foreign.Arena
import java.nio.file.Path

/**
 * vips-ffm FFM 바인딩 기반 [VipsImage] 구현체.
 *
 * 각 인스턴스는 `Arena.ofShared()`로 생성된 메모리 영역을 소유합니다.
 * `use {}` 블록 또는 `close()`로 반드시 리소스를 해제하십시오.
 *
 * vips-ffm의 연산은 새 [VImage] 인스턴스를 반환하는 함수형 모델입니다.
 * 각 연산 결과는 새 [FfmVipsImage]로 래핑됩니다.
 */
internal class FfmVipsImage(
    private val arena: Arena,
    private val vipsImage: VImage,
) : VipsImage {

    // 불변 프로퍼티: 생성 시점에 한 번만 읽어 캐싱합니다.
    override val width: Int = try { vipsImage.width } catch (e: VipsError) { throw VipsDecodeException("Failed to read width", e) }
    override val height: Int = try { vipsImage.height } catch (e: VipsError) { throw VipsDecodeException("Failed to read height", e) }
    override val bands: Int = try { vipsImage.getInt("bands") ?: 3 } catch (e: VipsError) { 3 }

    override fun resize(width: Int, height: Int): VipsImage {
        return try {
            val newArena = Arena.ofShared()
            val resized = resizeWithFfm(newArena, vipsImage, width, height, this.width, this.height)
            FfmVipsImage(newArena, resized)
        } catch (e: VipsError) {
            throw VipsDecodeException("Image resize failed", e)
        }
    }

    override fun thumbnail(maxDimension: Int): VipsImage {
        return try {
            val newArena = Arena.ofShared()
            val thumb = thumbnailWithFfm(newArena, vipsImage, maxDimension)
            FfmVipsImage(newArena, thumb)
        } catch (e: VipsError) {
            throw VipsDecodeException("Image thumbnail failed", e)
        }
    }

    override fun crop(left: Int, top: Int, width: Int, height: Int): VipsImage {
        return try {
            val newArena = Arena.ofShared()
            val cropped = vipsImage.extractArea(left, top, width, height)
            FfmVipsImage(newArena, cropped)
        } catch (e: VipsError) {
            throw VipsDecodeException("Image crop failed", e)
        }
    }

    override fun toBytes(format: VipsImageFormat, options: VipsEncodeOptions): ByteArray {
        return try {
            when (format) {
                VipsImageFormat.JPEG -> FfmVipsJpegWriter.writeToBytes(vipsImage, options)
                VipsImageFormat.PNG  -> FfmVipsPngWriter.writeToBytes(vipsImage, options)
                VipsImageFormat.WEBP -> FfmVipsWebpWriter.writeToBytes(vipsImage, options)
                else                 -> throw VipsEncodeException("Unsupported format for encoding: $format")
            }
        } catch (e: VipsError) {
            throw VipsEncodeException("Image encoding failed: $format", e)
        }
    }

    override fun writeTo(path: Path, format: VipsImageFormat, options: VipsEncodeOptions) {
        try {
            val bytes = toBytes(format, options)
            path.toFile().writeBytes(bytes)
        } catch (e: VipsEncodeException) {
            throw e
        } catch (e: Exception) {
            throw VipsEncodeException("Failed to write image to path", e)
        }
    }

    override fun writeTo(out: OutputStream, format: VipsImageFormat, options: VipsEncodeOptions) {
        try {
            val bytes = toBytes(format, options)
            out.write(bytes)
        } catch (e: VipsEncodeException) {
            throw e
        } catch (e: Exception) {
            throw VipsEncodeException("Failed to write image to output stream", e)
        }
    }

    override fun close() {
        arena.close()
    }
}
