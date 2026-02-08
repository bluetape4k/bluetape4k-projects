package io.bluetape4k.io

import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths

@TempFolderTest
class FileSupportResultTest {

    companion object: KLogging()

    private lateinit var tempFolder: TempFolder

    @BeforeAll
    fun setup(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @Test
    fun `tryCreateDirectory - 성공`() {
        val dirPath = Paths.get(tempFolder.rootPath, "test-dir").toString()

        val result = tryCreateDirectory(dirPath)

        result.isSuccess.shouldBeTrue()
        result.getOrThrow().exists().shouldBeTrue()
        result.getOrThrow().isDirectory.shouldBeTrue()
    }

    @Test
    fun `tryCreateDirectory - 이미 존재하는 디렉토리`() {
        val dirPath = Paths.get(tempFolder.rootPath, "existing-dir").toString()
        File(dirPath).mkdirs()

        val result = tryCreateDirectory(dirPath)

        result.isSuccess.shouldBeTrue()
        result.getOrThrow().absolutePath shouldBeEqualTo dirPath
    }

    @Test
    fun `tryCreateDirectory - 파일로 존재하는 경로`() {
        val filePath = Paths.get(tempFolder.rootPath, "test-file.txt").toString()
        File(filePath).createNewFile()

        val result = tryCreateDirectory(filePath)

        result.isFailure.shouldBeTrue()
    }

    @Test
    fun `tryCreateFile - 성공`() {
        val filePath = Paths.get(tempFolder.rootPath, "test.txt").toString()

        val result: Result<File> = tryCreateFile(filePath)

        result.isSuccess.shouldBeTrue()
        result.getOrThrow().exists().shouldBeTrue()
        result.getOrThrow().isFile.shouldBeTrue()
    }

    @Test
    fun `tryCreateFile - 부모 디렉토리 자동 생성`() {
        val filePath = Paths.get(tempFolder.rootPath, "sub/dir/test.txt").toString()
        val result = tryCreateFile(filePath)

        result.isSuccess.shouldBeTrue()
        File(filePath).parentFile.exists().shouldBeTrue()
    }

    @Test
    fun `tryCreateFile - 디렉토리로 존재하는 경로`() {
        val dirPath = Paths.get(tempFolder.rootPath, "test-dir").toString()
        File(dirPath).mkdirs()

        val result = tryCreateFile(dirPath)

        result.isFailure.shouldBeTrue()
    }

    @Test
    fun `tryDeleteRecursively - 성공`() {
        val dir = Paths.get(tempFolder.rootPath, "test-dir").toFile()
        dir.mkdirs()
        File(dir, "file1.txt").createNewFile()
        File(dir, "file2.txt").createNewFile()

        val result = dir.tryDeleteRecursively()

        result.isSuccess.shouldBeTrue()
        result.getOrThrow().shouldBeTrue()
        dir.exists().shouldBeEqualTo(false)
    }

    @Test
    fun `tryDeleteIfExists - 파일 존재`() {
        val file = Paths.get(tempFolder.rootPath, "test.txt").toFile()
        file.createNewFile()

        val result = file.tryDeleteIfExists()

        result.isSuccess.shouldBeTrue()
        file.exists().shouldBeEqualTo(false)
    }

    @Test
    fun `tryDeleteIfExists - 파일 없음`() {
        val file = Paths.get(tempFolder.rootPath, "non-existent.txt").toFile()

        val result = file.tryDeleteIfExists()

        // 파일이 없어도 성공
        result.isSuccess.shouldBeTrue()
        result.getOrThrow().shouldBeTrue()
    }

    @Test
    fun `tryReadAllBytes - 성공`() {
        val path = Paths.get(tempFolder.rootPath, "test.txt")
        val content = "Hello, World!".toByteArray()
        path.toFile().writeBytes(content)

        val result = path.tryReadAllBytes()

        result.isSuccess.shouldBeTrue()
        result.getOrThrow() shouldBeEqualTo content
    }

    @Test
    fun `tryReadAllBytes - 파일 없음`() {
        val path = Paths.get(tempFolder.rootPath, "non-existent.txt")
        val result = path.tryReadAllBytes()

        result.isFailure.shouldBeTrue()
    }

    @Test
    fun `tryWriteBytes - 성공`() {
        val path = Paths.get(tempFolder.rootPath, "output.txt")
        val content = "Test content".toByteArray()

        val result = path.tryWriteBytes(content)

        result.isSuccess.shouldBeTrue()
        result.getOrThrow() shouldBeEqualTo content.size.toLong()
        path.toFile().readBytes() shouldBeEqualTo content
    }

    @Test
    fun `tryWriteLines - 성공`() {
        val path = Paths.get(tempFolder.rootPath, "lines.txt")
        val lines = listOf("Line 1", "Line 2", "Line 3")

        val result = path.tryWriteLines(lines)

        result.isSuccess.shouldBeTrue()
        path.toFile().readLines() shouldContainSame lines
    }

    @Test
    fun `tryReadAllLines - 성공`() {
        val path = Paths.get(tempFolder.rootPath, "lines.txt")
        val lines = listOf("Line 1", "Line 2", "Line 3")
        path.toFile().writeText(lines.joinToString("\n"))

        val result = path.tryReadAllLines()

        result.isSuccess.shouldBeTrue()
        result.getOrThrow() shouldContainSame lines
    }

    @Test
    fun `tryCopyToAsync - 성공`() = runTest {
        val source = Paths.get(tempFolder.rootPath, "source.txt").toFile()
        source.writeText("Source content")
        val target = Paths.get(tempFolder.rootPath, "target.txt").toFile()

        val futureResult = source.tryCopyToAsync(target)
        val result = futureResult.await()

        result.isSuccess.shouldBeTrue()
        target.exists().shouldBeTrue()
        target.readText() shouldBeEqualTo "Source content"
    }

    @Test
    fun `tryCopyToAsync - 원본 파일 없음`() = runTest {
        val source = Paths.get(tempFolder.rootPath, "non-existence.txt").toFile()
        val target = Paths.get(tempFolder.rootPath, "target.txt").toFile()

        val futureResult = source.tryCopyToAsync(target)
        val result = futureResult.await()

        result.isFailure.shouldBeTrue()
    }

    @Test
    fun `tryMoveAsync - 성공`() = runTest {
        val source = Paths.get(tempFolder.rootPath, "move-async-source.txt").toFile()
        source.writeText("Move me")
        val target = Paths.get(tempFolder.rootPath, "move-async-target.txt").toFile()

        val futureResult = source.tryMoveAsync(target)
        val result = futureResult.await()

        result.isSuccess.shouldBeTrue()
        source.exists().shouldBeEqualTo(false)
        target.exists().shouldBeTrue()
        target.readText() shouldBeEqualTo "Move me"
    }

    @Test
    fun `tryReadAllBytesAsync - 성공`() = runTest {
        val path = Paths.get(tempFolder.rootPath, "async-read-all.txt")
        val content = "Async content".toByteArray()
        path.toFile().writeBytes(content)

        val futureResult = path.tryReadAllBytesAsync()
        val result: Result<ByteArray> = futureResult.await()

        result.isSuccess.shouldBeTrue()
        result.getOrThrow() shouldContainSame content
    }

    @Test
    fun `tryWriteAsync - 성공`() = runTest {
        val path = Paths.get(tempFolder.rootPath, "async-write.txt")
        val content = "Async write".toByteArray()

        val futureResult = path.tryWriteAsync(content)
        val result = futureResult.await()

        result.isSuccess.shouldBeTrue()
        path.toFile().readBytes() shouldBeEqualTo content
    }

    @Test
    fun `tryWriteLinesAsync - 성공`() = runTest {
        val path = Paths.get(tempFolder.rootPath, "async-lines.txt")
        val lines = listOf("Async line 1", "Async line 2")

        val futureResult = path.tryWriteLinesAsync(lines)
        val result = futureResult.await()

        result.isSuccess.shouldBeTrue()
        path.toFile().readLines() shouldContainSame lines
    }

    @Test
    fun `함수형 체이닝 예제`() {
        val path = Paths.get(tempFolder.rootPath, "config.txt")

        // 파일 생성 -> 쓰기 -> 읽기 체인
        val result = tryCreateFile(path.toString())
            .mapCatching { file ->
                file.writeText("key=value")
                file
            }
            .mapCatching { file ->
                file.readText()
            }
            .map { content ->
                content.split("=").last()
            }

        result.isSuccess.shouldBeTrue()
        result.getOrThrow() shouldBeEqualTo "value"
    }

    @Test
    fun `에러 핸들링 예제`() {
        val path = Paths.get(tempFolder.rootPath, "non-existent.txt")

        val result = path.tryReadAllBytes()
            .recoverCatching { error ->
                // 실패 시 빈 바이트 배열 반환
                ByteArray(0)
            }

        result.isSuccess.shouldBeTrue()
        result.getOrThrow() shouldBeEqualTo ByteArray(0)
    }
}
