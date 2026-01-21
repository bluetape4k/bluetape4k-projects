package io.bluetape4k.images

import com.sksamuel.scrimage.nio.WriteContext
import java.io.ByteArrayOutputStream

/**
 * [WriteContext]를 [ByteArray]로 변환합니다.
 *
 * ```
 * val bytes = writeContext.toByteArray()
 * ```
 *
 * @return [ByteArray]
 */
fun WriteContext.toByteArray(): ByteArray =
    ByteArrayOutputStream().use { bos ->
        this@toByteArray.write(bos)
        bos.toByteArray()
    }
