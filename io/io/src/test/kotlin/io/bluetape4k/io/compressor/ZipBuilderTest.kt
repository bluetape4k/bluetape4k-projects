package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

class ZipBuilderTest {

    companion object: KLogging()

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `인메모리 ZIP 생성 - 문자열 콘텐트`() {
        val content = "Hello, ZipBuilder!"

        val bytes = ZipBuilder.ofInMemory()
            .add(content).path("hello.txt").save()
            .toBytes()

        bytes.size shouldBeGreaterThan 0
        log.debug { "인메모리 ZIP 크기: ${bytes.size}" }

        // ZIP 파일로 저장하여 검증
        val zipFile = File(tempDir, "test.zip")
        zipFile.writeBytes(bytes)

        ZipFile(zipFile).use { zip ->
            val entry = zip.getEntry("hello.txt")
            entry.shouldNotBeNull()
            val extracted = zip.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
            extracted shouldBeEqualTo content
        }
    }

    @Test
    fun `인메모리 ZIP 생성 - 바이트 배열 콘텐트`() {
        val content = "바이트 배열 테스트".toByteArray(Charsets.UTF_8)

        val bytes = ZipBuilder.ofInMemory()
            .add(content).path("data.bin").save()
            .toBytes()

        bytes.size shouldBeGreaterThan 0

        val zipFile = File(tempDir, "bytes.zip")
        zipFile.writeBytes(bytes)

        ZipFile(zipFile).use { zip ->
            val entry = zip.getEntry("data.bin")
            entry.shouldNotBeNull()
            zip.getInputStream(entry).readBytes() shouldBeEqualTo content
        }
    }

    @Test
    fun `파일 기반 ZIP 생성`() {
        val sourceFile = File(tempDir, "source.txt")
        sourceFile.writeText("파일 기반 ZIP 테스트", Charsets.UTF_8)

        val zipFile = File(tempDir, "file-based.zip")

        val result = ZipBuilder.of(zipFile)
            .add(sourceFile).path("source.txt").save()
            .toZipFile()

        result.shouldNotBeNull()
        result.exists().shouldBeTrue()

        ZipFile(result).use { zip ->
            val entry = zip.getEntry("source.txt")
            entry.shouldNotBeNull()
            val extracted = zip.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
            extracted shouldBeEqualTo "파일 기반 ZIP 테스트"
        }
    }

    @Test
    fun `여러 콘텐트 추가`() {
        val bytes = ZipBuilder.ofInMemory()
            .add("first content").path("first.txt").save()
            .add("second content").path("second.txt").save()
            .toBytes()

        val zipFile = File(tempDir, "multi.zip")
        zipFile.writeBytes(bytes)

        ZipFile(zipFile).use { zip ->
            zip.getEntry("first.txt").shouldNotBeNull()
            zip.getEntry("second.txt").shouldNotBeNull()

            val first = zip.getInputStream(zip.getEntry("first.txt")).readBytes().toString(Charsets.UTF_8)
            first shouldBeEqualTo "first content"
        }
    }

    @Test
    fun `폴더 엔트리 추가`() {
        val bytes = ZipBuilder.ofInMemory()
            .addFolder("docs")
            .add("readme content").path("docs/readme.txt").save()
            .toBytes()

        val zipFile = File(tempDir, "folder.zip")
        zipFile.writeBytes(bytes)

        ZipFile(zipFile).use { zip ->
            val folderEntry = zip.getEntry("docs/")
            folderEntry.shouldNotBeNull()
            folderEntry.isDirectory.shouldBeTrue()

            val fileEntry = zip.getEntry("docs/readme.txt")
            fileEntry.shouldNotBeNull()
        }
    }

    @Test
    fun `디렉토리 재귀 추가`() {
        // 디렉토리 구조 생성
        val subDir = File(tempDir, "src")
        subDir.mkdirs()
        File(subDir, "a.txt").writeText("file A")
        File(subDir, "b.txt").writeText("file B")

        val zipFile = File(tempDir, "recursive.zip")

        ZipBuilder.of(zipFile)
            .add(subDir).path("src/").recursive(true).save()
            .toZipFile()

        ZipFile(zipFile).use { zip ->
            val entries = zip.entries().toList().map { it.name }
            entries.any { it.contains("a.txt") }.shouldBeTrue()
            entries.any { it.contains("b.txt") }.shouldBeTrue()
        }
    }
}
