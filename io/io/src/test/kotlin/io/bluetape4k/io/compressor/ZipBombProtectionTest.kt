package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertFailsWith

/**
 * [unzip] zip bomb 방어 테스트.
 *
 * - 엔트리 수 한도 초과 시 [IllegalArgumentException] 발생 검증
 * - [ZIP_MAX_ENTRIES], [ZIP_MAX_UNCOMPRESSED_SIZE] 상수 값 검증
 */
class ZipBombProtectionTest {

    companion object: KLogging()

    @TempDir
    lateinit var tempDir: File

    // ────────────────────────────────────────────────────────────────────────────
    // 상수 검증
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `ZIP_MAX_ENTRIES 상수는 10_000 이다`() {
        ZIP_MAX_ENTRIES shouldBeEqualTo 10_000
    }

    @Test
    fun `ZIP_MAX_UNCOMPRESSED_SIZE 상수는 1GB 이다`() {
        ZIP_MAX_UNCOMPRESSED_SIZE shouldBeEqualTo 1L * 1024 * 1024 * 1024
    }

    @Test
    fun `ZIP_MAX_ENTRIES 는 양수이다`() {
        ZIP_MAX_ENTRIES shouldBeGreaterThan 0
    }

    @Test
    fun `ZIP_MAX_UNCOMPRESSED_SIZE 는 양수이다`() {
        ZIP_MAX_UNCOMPRESSED_SIZE shouldBeGreaterThan 0L
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 엔트리 수 한도 초과
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `엔트리 수가 ZIP_MAX_ENTRIES 를 초과하면 IllegalArgumentException 발생`() {
        val excessCount = ZIP_MAX_ENTRIES + 1
        val zipFile = createZipWithManyEntries(excessCount, contentBytes = "x".toByteArray())

        val destDir = File(tempDir, "bomb-entries-out")
        destDir.mkdirs()

        assertFailsWith<IllegalArgumentException>("엔트리 수 한도 초과 시 예외가 발생해야 한다") {
            unzip(zipFile, destDir)
        }
    }

    @Test
    fun `엔트리 수가 ZIP_MAX_ENTRIES 이하이면 정상 처리된다`() {
        val zipFile = createZipWithManyEntries(count = 3, contentBytes = "hello".toByteArray())

        val destDir = File(tempDir, "entries-ok-out")
        destDir.mkdirs()

        // 예외 없이 완료해야 한다
        unzip(zipFile, destDir)

        require(destDir.list()?.isNotEmpty() == true) { "추출 결과가 존재해야 한다" }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 비압축 크기 한도: 단위 테스트 (실제 1GB 데이터 없이 검증)
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `ZIP_MAX_UNCOMPRESSED_SIZE 는 ZIP_MAX_ENTRIES 파일을 수용할 만큼 충분히 크다`() {
        // 1GB 한도 / 10_000 엔트리 = 파일당 평균 100KB 허용
        val avgBytesPerEntry = ZIP_MAX_UNCOMPRESSED_SIZE / ZIP_MAX_ENTRIES
        require(avgBytesPerEntry > 0L) { "엔트리당 평균 허용 크기가 양수여야 한다" }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 헬퍼: 다수의 엔트리를 가진 ZIP 파일 생성
    // ────────────────────────────────────────────────────────────────────────────

    private fun createZipWithManyEntries(count: Int, contentBytes: ByteArray): File {
        val zipFile = File(tempDir, "many-entries-$count.zip")
        ZipOutputStream(zipFile.outputStream()).use { zos ->
            repeat(count) { i ->
                val entry = ZipEntry("file_$i.txt")
                zos.putNextEntry(entry)
                zos.write(contentBytes)
                zos.closeEntry()
            }
        }
        return zipFile
    }
}
