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
import java.io.ByteArrayOutputStream
import java.nio.file.Path

@TempFolderTest
class SuspendTiffMultiPageWriterTest : AbstractImageTest() {

    companion object : KLoggingChannel() {
        @JvmStatic
        @BeforeAll
        fun registerSpis() {
            IIORegistryUtils.registerApplicationClasspathSpis()
        }
    }

    @Test
    fun `단일 이미지를 단일 페이지 TIFF로 쓰기`() = runSuspendIO {
        val image = immutableImageOf(Path.of("$BASE_PATH/homer.jpg"))
        val writer = SuspendTiffMultiPageWriter.Default
        val bos = ByteArrayOutputStream()

        writer.suspendWrite(image, bos)

        bos.size() shouldBeGreaterThan 0
        log.debug { "단일 페이지 TIFF: ${bos.size()} bytes" }
    }

    @Test
    fun `복수 이미지를 다중 페이지 TIFF로 쓰기`() = runSuspendIO {
        val image1 = immutableImageOf(Path.of("$BASE_PATH/homer.jpg"))
        val image2 = immutableImageOf(Path.of("$BASE_PATH/labor.jpg"))
        val image3 = immutableImageOf(Path.of("$BASE_PATH/cafe.jpg"))
        val writer = SuspendTiffMultiPageWriter.Default
        val bos = ByteArrayOutputStream()

        writer.suspendWrite(listOf(image1, image2, image3), bos)

        bos.size() shouldBeGreaterThan 0
        log.debug { "3-페이지 TIFF: ${bos.size()} bytes" }
    }

    @Test
    fun `LZW 압축으로 다중 페이지 TIFF 쓰기`() = runSuspendIO {
        val image1 = immutableImageOf(Path.of("$BASE_PATH/homer.jpg"))
        val image2 = immutableImageOf(Path.of("$BASE_PATH/labor.jpg"))
        val writer = SuspendTiffMultiPageWriter(compression = TiffCompression.LZW)
        val bos = ByteArrayOutputStream()

        writer.suspendWrite(listOf(image1, image2), bos)

        bos.size() shouldBeGreaterThan 0
        log.debug { "LZW 2-페이지 TIFF: ${bos.size()} bytes" }
    }

    @Test
    fun `TIFF 파일로 저장`(tempFolder: TempFolder) = runSuspendIO {
        val image1 = immutableImageOf(Path.of("$BASE_PATH/homer.jpg"))
        val image2 = immutableImageOf(Path.of("$BASE_PATH/labor.jpg"))
        val dest = tempFolder.createFile("multipage.tiff")
        val writer = SuspendTiffMultiPageWriter.Default
        val bos = ByteArrayOutputStream()

        writer.suspendWrite(listOf(image1, image2), bos)
        dest.writeBytes(bos.toByteArray())

        dest.exists().shouldBeTrue()
        dest.length() shouldBeGreaterThan 0L
        log.debug { "저장됨: ${dest.absolutePath} (${dest.length()} bytes)" }
    }

    @Test
    fun `빈 이미지 리스트 예외 발생`() = runSuspendIO {
        val writer = SuspendTiffMultiPageWriter.Default
        val bos = ByteArrayOutputStream()

        try {
            writer.suspendWrite(emptyList(), bos)
            throw AssertionError("예외가 발생해야 합니다")
        } catch (e: IllegalArgumentException) {
            log.debug { "예상된 예외: ${e.message}" }
        }
    }

    @Test
    fun `maxPages 초과 시 예외 발생`() = runSuspendIO {
        val image = immutableImageOf(Path.of("$BASE_PATH/homer.jpg"))
        val writer = SuspendTiffMultiPageWriter(maxPages = 2)
        val bos = ByteArrayOutputStream()

        try {
            writer.suspendWrite(listOf(image, image, image), bos)
            throw AssertionError("예외가 발생해야 합니다")
        } catch (e: IllegalArgumentException) {
            log.debug { "예상된 예외: ${e.message}" }
        }
    }
}
