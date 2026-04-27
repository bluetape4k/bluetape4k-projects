package io.bluetape4k.images.coroutines

import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.IIORegistryUtils
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.nio.file.Path

@TempFolderTest
class SuspendTiffWriterTest : AbstractImageTest() {

    companion object : KLoggingChannel() {
        @JvmStatic
        @BeforeAll
        fun registerSpis() {
            IIORegistryUtils.registerApplicationClasspathSpis()
        }
    }

    @ParameterizedTest
    @MethodSource("getImageFileNames")
    fun `TIFF 기본 압축으로 이미지 쓰기`(filename: String, tempFolder: TempFolder) = runSuspendIO {
        val image = immutableImageOf(Path.of("$BASE_PATH/$filename.jpg"))
        val writer = SuspendTiffWriter.Default
        val bos = ByteArrayOutputStream()

        writer.suspendWrite(image, bos)

        bos.size() shouldBeGreaterThan 0
        log.debug { "$filename TIFF (DEFLATE): ${bos.size()} bytes" }
    }

    @ParameterizedTest
    @MethodSource("getImageFileNames")
    fun `TIFF LZW 압축으로 이미지 쓰기`(filename: String, tempFolder: TempFolder) = runSuspendIO {
        val image = immutableImageOf(Path.of("$BASE_PATH/$filename.jpg"))
        val writer = SuspendTiffWriter.Lzw
        val bos = ByteArrayOutputStream()

        writer.suspendWrite(image, bos)

        bos.size() shouldBeGreaterThan 0
        log.debug { "$filename TIFF (LZW): ${bos.size()} bytes" }
    }

    @ParameterizedTest
    @MethodSource("getImageFileNames")
    fun `TIFF 무압축으로 이미지 쓰기`(filename: String, tempFolder: TempFolder) = runSuspendIO {
        val image = immutableImageOf(Path.of("$BASE_PATH/$filename.jpg"))
        val writer = SuspendTiffWriter.Uncompressed
        val bos = ByteArrayOutputStream()

        writer.suspendWrite(image, bos)

        bos.size() shouldBeGreaterThan 0
        log.debug { "$filename TIFF (NONE): ${bos.size()} bytes" }
    }

    @ParameterizedTest
    @MethodSource("getImageFileNames")
    fun `TIFF 파일로 저장`(filename: String, tempFolder: TempFolder) = runSuspendIO {
        val image = immutableImageOf(Path.of("$BASE_PATH/$filename.jpg"))
        val dest = tempFolder.createFile("$filename.tiff")
        val writer = SuspendTiffWriter.Default
        val bos = ByteArrayOutputStream()

        writer.suspendWrite(image, bos)
        dest.writeBytes(bos.toByteArray())

        dest.exists().shouldBeTrue()
        dest.length() shouldBeGreaterThan 0L
        log.debug { "저장됨: ${dest.absolutePath} (${dest.length()} bytes)" }
    }

    @Test
    fun `quality 범위 초과 시 예외 발생`() {
        try {
            SuspendTiffWriter(quality = 1.5f)
            throw AssertionError("예외가 발생해야 합니다")
        } catch (e: IllegalArgumentException) {
            log.debug { "예상된 예외: ${e.message}" }
        }
    }

    @Test
    fun `quality 음수 시 예외 발생`() {
        try {
            SuspendTiffWriter(quality = -0.1f)
            throw AssertionError("예외가 발생해야 합니다")
        } catch (e: IllegalArgumentException) {
            log.debug { "예상된 예외: ${e.message}" }
        }
    }
}
