package io.bluetape4k.images

import com.sksamuel.scrimage.nio.WriteContext
import java.io.ByteArrayOutputStream

/**
 * [WriteContext]를 [ByteArray]로 변환합니다.
 *
 * ```kotlin
 * val writer = SuspendJpegWriter.Default
 * val image = immutableImageOf(File("photo.jpg"))
 * val context = image.forWriter(writer)
 * val bytes = context.toByteArray()
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @return [ByteArray]
 */
fun WriteContext.toByteArray(): ByteArray =
    ByteArrayOutputStream().use { bos ->
        this@toByteArray.write(bos)
        bos.toByteArray()
    }
