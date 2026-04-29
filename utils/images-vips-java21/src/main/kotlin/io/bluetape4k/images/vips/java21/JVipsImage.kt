package io.bluetape4k.images.vips.java21

import io.bluetape4k.images.vips.VipsEncodeException
import io.bluetape4k.images.vips.VipsEncodeOptions
import io.bluetape4k.images.vips.VipsImage
import io.bluetape4k.images.vips.VipsImageFormat
import io.bluetape4k.images.vips.VipsOperationException
import io.bluetape4k.images.vips.java21.internal.NativeHandle
import io.bluetape4k.images.vips.java21.ops.resizeWithJVips
import io.bluetape4k.images.vips.java21.ops.thumbnailWithJVips
import io.bluetape4k.images.vips.java21.writer.JVipsJpegWriter
import io.bluetape4k.images.vips.java21.writer.JVipsPngWriter
import io.bluetape4k.images.vips.java21.writer.JVipsWebpWriter
import com.criteo.vips.VipsException
import kotlinx.coroutines.CancellationException
import java.awt.Rectangle
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * JVips JNI 바인딩 기반 [VipsImage] 구현체.
 *
 * 모든 연산(resize/thumbnail/crop)은 원본을 변경하지 않으며 새 인스턴스를 반환합니다.
 * JVips `VipsImage`는 in-place 변이 모델이므로 각 연산 전에 `clone()`이 필요합니다.
 *
 * `use {}` 블록 또는 `close()`로 반드시 리소스를 해제하십시오.
 */
internal class JVipsImage(private val handle: NativeHandle) : VipsImage {

    private val closed = AtomicBoolean(false)

    // 불변 프로퍼티: 생성 시점에 한 번만 읽어 캐싱합니다.
    override val width: Int = handle.vipsImage.width
    override val height: Int = handle.vipsImage.height
    override val bands: Int = handle.vipsImage.bands

    private fun checkOpen() {
        if (closed.get()) throw IllegalStateException("VipsImage has been closed")
    }

    override fun resize(width: Int, height: Int): VipsImage {
        checkOpen()
        val cloned = handle.vipsImage.clone()
        var nativeHandle: NativeHandle? = null
        return try {
            resizeWithJVips(cloned, width, height)
            nativeHandle = NativeHandle(cloned)
            JVipsImage(nativeHandle)
        } catch (e: Exception) {
            // NativeHandle 생성 여부에 따라 해제 방식 결정:
            // - 생성됐으면 close() 사용 (Cleaner-aware, 이중 해제 방지)
            // - 미생성이면 직접 release()
            nativeHandle?.close() ?: cloned.release()
            when (e) {
                is VipsException -> throw VipsOperationException("Image resize failed", e)
                else -> throw e
            }
        }
    }

    override fun thumbnail(maxDimension: Int): VipsImage {
        checkOpen()
        val cloned = handle.vipsImage.clone()
        var nativeHandle: NativeHandle? = null
        return try {
            thumbnailWithJVips(cloned, maxDimension)
            nativeHandle = NativeHandle(cloned)
            JVipsImage(nativeHandle)
        } catch (e: Exception) {
            nativeHandle?.close() ?: cloned.release()
            when (e) {
                is VipsException -> throw VipsOperationException("Image thumbnail failed", e)
                else -> throw e
            }
        }
    }

    override fun crop(left: Int, top: Int, width: Int, height: Int): VipsImage {
        checkOpen()
        val cloned = handle.vipsImage.clone()
        var nativeHandle: NativeHandle? = null
        return try {
            cloned.crop(Rectangle(left, top, width, height))
            nativeHandle = NativeHandle(cloned)
            JVipsImage(nativeHandle)
        } catch (e: Exception) {
            nativeHandle?.close() ?: cloned.release()
            when (e) {
                is VipsException -> throw VipsOperationException("Image crop failed", e)
                else -> throw e
            }
        }
    }

    override fun toBytes(format: VipsImageFormat, options: VipsEncodeOptions): ByteArray {
        checkOpen()
        return try {
            when (format) {
                VipsImageFormat.JPEG -> JVipsJpegWriter.writeToBytes(handle.vipsImage, options)
                VipsImageFormat.PNG  -> JVipsPngWriter.writeToBytes(handle.vipsImage, options)
                VipsImageFormat.WEBP -> JVipsWebpWriter.writeToBytes(handle.vipsImage, options)
                else                 -> throw VipsEncodeException("Unsupported format for encoding: $format")
            }
        } catch (e: VipsEncodeException) {
            throw e
        } catch (e: VipsException) {
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
        if (closed.compareAndSet(false, true)) {
            handle.close()
        }
    }
}
