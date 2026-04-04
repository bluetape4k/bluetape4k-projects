package io.bluetape4k.io.compressor

import io.bluetape4k.support.requirePositiveNumber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * JDK ZIP 알고리즘을 이용한 압축/복원
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, Zip!".toByteArray()
 * val compressed = Compressors.Zip.compress(data)
 * val restored = Compressors.Zip.decompress(compressed)
 * // restored contentEquals data == true
 * ```
 *
 * @see [ZipOutputStream]
 * @see [ZipInputStream]
 */
class ZipCompressor(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
): AbstractCompressor() {

    init {
        bufferSize.requirePositiveNumber("bufferSize")
    }

    /**
     * [plain] 데이터를 ZIP 형식으로 압축합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(plain.size)
        ZipOutputStream(output).use { zos ->
            zos.putNextEntry(ZipEntry("data"))
            zos.write(plain)
            zos.closeEntry()
            zos.finish()
        }
        return output.toByteArray()
    }

    /**
     * ZIP 형식으로 압축된 [compressed] 데이터를 복원합니다.
     * 첫 번째 엔트리의 데이터를 반환합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            ZipInputStream(input).use { zis ->
                zis.nextEntry ?: throw java.io.IOException("No entry found in ZIP data")
                val output = ByteArrayOutputStream(bufferSize)
                val buffer = ByteArray(bufferSize)
                var len: Int
                while (zis.read(buffer).also { len = it } != -1) {
                    output.write(buffer, 0, len)
                }
                output.toByteArray()
            }
        }
    }
}
