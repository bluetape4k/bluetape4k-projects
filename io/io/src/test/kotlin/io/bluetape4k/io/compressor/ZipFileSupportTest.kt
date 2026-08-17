package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.closeSafe
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import io.bluetape4k.assertions.assertFailsWith

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
    fun `Zip Slip 방어는 대상 디렉토리와 같은 접두어의 형제 경로를 거부한다`() {
        val zipBytes = ZipBuilder.ofInMemory()
            .add("sibling content").path("../safe-output-sibling/evil.txt").save()
            .toBytes()

        val zipFile = File(tempDir, "sibling.zip")
        zipFile.writeBytes(zipBytes)

        val destDir = File(tempDir, "safe-output")
        destDir.mkdirs()

        assertFailsWith<IllegalArgumentException> {
            unzip(zipFile, destDir)
        }

        File(tempDir, "safe-output-sibling/evil.txt").exists() shouldBeEqualTo false
    }

    @Test
    fun `unzip은 기존 출력 디렉토리 심볼릭 링크를 따라가지 않는다`() {
        val outsideDir = File(tempDir.parentFile, "zip-slip-outside-dir").apply { mkdirs() }
        val destDir = File(tempDir, "symlink-output").apply { mkdirs() }
        Files.createSymbolicLink(File(destDir, "linked").toPath(), outsideDir.toPath())

        val zipFile = File(tempDir, "symlink-directory.zip")
        zipFile.writeBytes(
            ZipBuilder.ofInMemory()
                .add("악성 내용").path("linked/evil.txt").save()
                .toBytes(),
        )

        assertFailsWith<IOException> {
            unzip(zipFile, destDir)
        }

        File(outsideDir, "evil.txt").exists() shouldBeEqualTo false
    }

    @Test
    fun `unzip은 기존 출력 파일 심볼릭 링크를 덮어쓰지 않는다`() {
        val outsideFile = File(tempDir.parentFile, "zip-slip-outside-file.txt").apply {
            writeText("기존 안전한 내용")
        }
        val destDir = File(tempDir, "symlink-file-output").apply { mkdirs() }
        Files.createSymbolicLink(File(destDir, "escape.txt").toPath(), outsideFile.toPath())

        val zipFile = File(tempDir, "symlink-file.zip")
        zipFile.writeBytes(
            ZipBuilder.ofInMemory()
                .add("악성 덮어쓰기").path("escape.txt").save()
                .toBytes(),
        )

        assertFailsWith<IOException> {
            unzip(zipFile, destDir)
        }

        outsideFile.readText() shouldBeEqualTo "기존 안전한 내용"
    }

    @Test
    fun `unzip은 대상 디렉토리의 조상 심볼릭 링크를 따라가지 않는다`() {
        val outsideDir = File(tempDir.parentFile, "zip-slip-ancestor-outside-${System.nanoTime()}").apply {
            mkdirs()
        }
        val destinationParent = File(tempDir, "destination-parent").apply { mkdirs() }
        Files.createSymbolicLink(
            File(destinationParent, "redirect").toPath(),
            outsideDir.toPath(),
        )
        val destDir = File(destinationParent, "redirect/extracted")

        val zipFile = File(tempDir, "symlink-ancestor.zip")
        zipFile.writeBytes(
            ZipBuilder.ofInMemory()
                .add("악성 조상 경로 쓰기").path("evil.txt").save()
                .toBytes(),
        )

        assertFailsWith<IOException> {
            unzip(zipFile, destDir)
        }

        File(outsideDir, "extracted/evil.txt").exists() shouldBeEqualTo false
    }

    @Test
    fun `unzip은 중간 디렉토리 교체 경합에서 외부 파일을 쓰지 않는다`() {
        val outsideDir = File(tempDir.parentFile, "zip-slip-race-outside").apply { mkdirs() }
        val outsideFile = File(outsideDir, "evil.txt").apply { writeText("기존 안전한 내용") }
        val destDir = File(tempDir, "race-output").apply { mkdirs() }
        File(destDir, "nested").mkdirs()
        val parkedDir = File(destDir, "nested-parked")
        val payload = ByteArray(8 * 1024 * 1024) { 'A'.code.toByte() }
        val zipFile = File(tempDir, "symlink-directory-race.zip")
        val builder = ZipBuilder.ofInMemory()
        repeat(4) { index ->
            builder.add(payload).path("nested/evil-$index.txt").save()
        }
        zipFile.writeBytes(builder.toBytes())

        val ready = CountDownLatch(2)
        val stop = AtomicBoolean(false)
        MultithreadingTester()
            .workers(2)
            .rounds(1)
            .add {
                ready.countDown()
                ready.await(5, TimeUnit.SECONDS).shouldBeTrue()
                try {
                    unzip(zipFile, destDir)
                } catch (_: IOException) {
                    // 경합으로 추출이 중단되는 것은 허용하지만 외부 쓰기는 허용하지 않는다.
                } finally {
                    stop.set(true)
                }
            }
            .add {
                ready.countDown()
                ready.await(5, TimeUnit.SECONDS).shouldBeTrue()
                while (!stop.get()) {
                    try {
                        val nested = File(destDir, "nested").toPath()
                        val parked = parkedDir.toPath()
                        if (Files.exists(nested, LinkOption.NOFOLLOW_LINKS)) {
                            if (!Files.exists(parked, LinkOption.NOFOLLOW_LINKS)) {
                                Files.move(nested, parked, StandardCopyOption.ATOMIC_MOVE)
                            }
                            if (!Files.isSymbolicLink(nested)) {
                                Files.createSymbolicLink(nested, outsideDir.toPath())
                            }
                        } else if (Files.isSymbolicLink(nested)) {
                            Files.deleteIfExists(nested)
                            if (Files.exists(parked, LinkOption.NOFOLLOW_LINKS)) {
                                Files.move(parked, nested, StandardCopyOption.ATOMIC_MOVE)
                            }
                        }
                    } catch (_: IOException) {
                        // 추출기와 디렉토리 교체가 동시에 진행되는 동안의 정상적인 충돌이다.
                    }
                }
            }
            .run()

        outsideFile.readText() shouldBeEqualTo "기존 안전한 내용"
    }

    @Test
    fun `ZipFile closeSafe 는 null 에 대해 안전하다`() {
        val nullZip: java.util.zip.ZipFile? = null
        nullZip?.closeSafe() // 예외 없이 실행
    }
}
