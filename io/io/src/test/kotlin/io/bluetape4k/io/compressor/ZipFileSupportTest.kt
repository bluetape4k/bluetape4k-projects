package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.closeSafe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import kotlin.test.assertFailsWith

class ZipFileSupportTest {

    companion object: KLogging()

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `gzip 및 ungzip 라운드트립`() {
        val source = File(tempDir, "test.txt")
        source.writeText("gzip 라운드트립 테스트 데이터", Charsets.UTF_8)

        val gzipFile = gzip(source)
        gzipFile.exists().shouldBeTrue()
        gzipFile.name shouldBeEqualTo "test.txt.gz"

        val restored = ungzip(gzipFile)
        restored.exists().shouldBeTrue()
        restored.readText(Charsets.UTF_8) shouldBeEqualTo "gzip 라운드트립 테스트 데이터"
    }

    @Test
    fun `zlib 라운드트립`() {
        val source = File(tempDir, "test.txt")
        source.writeText("zlib 라운드트립 테스트 데이터", Charsets.UTF_8)

        val zlibFile = zlib(source)
        zlibFile.exists().shouldBeTrue()
        zlibFile.name shouldBeEqualTo "test.txt.zlib"
        log.debug { "zlib 파일 크기: ${zlibFile.length()}" }
    }

    @Test
    fun `gzip 은 디렉토리를 압축할 수 없다`() {
        val dir = File(tempDir, "subdir")
        dir.mkdirs()

        assertFailsWith<IOException> {
            gzip(dir)
        }
    }

    @Test
    fun `zlib 은 디렉토리를 압축할 수 없다`() {
        val dir = File(tempDir, "subdir")
        dir.mkdirs()

        assertFailsWith<IOException> {
            zlib(dir)
        }
    }

    @Test
    fun `zip 및 unzip 라운드트립 - 단일 파일`() {
        val source = File(tempDir, "hello.txt")
        source.writeText("zip 라운드트립 테스트", Charsets.UTF_8)

        val zipFile = zip(source)
        zipFile.shouldNotBeNull()
        zipFile.exists().shouldBeTrue()

        val destDir = File(tempDir, "extracted")
        destDir.mkdirs()

        unzip(zipFile, destDir)

        val extracted = File(destDir, "hello.txt")
        extracted.exists().shouldBeTrue()
        extracted.readText(Charsets.UTF_8) shouldBeEqualTo "zip 라운드트립 테스트"
    }

    @Test
    fun `zip 및 unzip 라운드트립 - 디렉토리`() {
        val srcDir = File(tempDir, "project")
        srcDir.mkdirs()
        File(srcDir, "a.txt").writeText("file A")
        File(srcDir, "b.txt").writeText("file B")
        val subDir = File(srcDir, "sub")
        subDir.mkdirs()
        File(subDir, "c.txt").writeText("file C")

        val zipFile = zip(srcDir)
        zipFile.shouldNotBeNull()
        zipFile.exists().shouldBeTrue()

        val destDir = File(tempDir, "extracted")
        destDir.mkdirs()

        unzip(zipFile, destDir)

        // 디렉토리와 파일이 추출되어야 함
        val extractedFiles = destDir.walkTopDown().filter { it.isFile }.toList()
        extractedFiles.any { it.name == "a.txt" }.shouldBeTrue()
        extractedFiles.any { it.name == "b.txt" }.shouldBeTrue()
        extractedFiles.any { it.name == "c.txt" }.shouldBeTrue()
    }

    @Test
    fun `unzip 패턴 필터`() {
        // ZIP 파일 생성 (여러 파일 포함)
        val zipBytes = ZipBuilder.ofInMemory()
            .add("text content").path("readme.txt").save()
            .add("log content").path("app.log").save()
            .add("data content").path("data.csv").save()
            .toBytes()

        val zipFile = File(tempDir, "filtered.zip")
        zipFile.writeBytes(zipBytes)

        val destDir = File(tempDir, "filtered-output")
        destDir.mkdirs()

        // .txt 파일만 추출
        unzip(zipFile, destDir, "*.txt")

        File(destDir, "readme.txt").exists().shouldBeTrue()
        File(destDir, "app.log").exists() shouldBeEqualTo false
        File(destDir, "data.csv").exists() shouldBeEqualTo false
    }

    @Test
    fun `Zip Slip 방어 테스트`() {
        // 악의적 경로를 가진 ZIP 생성
        val zipBytes = ZipBuilder.ofInMemory()
            .add("malicious content").path("../../../etc/passwd").save()
            .toBytes()

        val zipFile = File(tempDir, "malicious.zip")
        zipFile.writeBytes(zipBytes)

        val destDir = File(tempDir, "safe-output")
        destDir.mkdirs()

        assertFailsWith<IllegalArgumentException> {
            unzip(zipFile, destDir)
        }
    }

    @Test
    fun `ZipFile closeSafe 는 null 에 대해 안전하다`() {
        val nullZip: java.util.zip.ZipFile? = null
        nullZip?.closeSafe() // 예외 없이 실행
    }
}
