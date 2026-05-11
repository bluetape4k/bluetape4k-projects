package io.bluetape4k.junit5.tempfolder

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class TempFolderClassTest {
    companion object: KLogging()

    @Test
    fun `임시 폴더 생성 후 close 시에 임시폴더는 삭제된다`() {
        val tempFolder = TempFolder()
        val root = tempFolder.root

        root.exists().shouldBeTrue()

        tempFolder.createDirectory("tempDir")
        tempFolder.createFile("tempFile")
        val tempFile = tempFolder.createFile()
        tempFile.exists().shouldBeTrue()

        tempFolder.close()
        root.exists().shouldBeFalse()
    }

    @Test
    fun `유효하지 않는 폴더명으로 생성하기`() {
        val invalidDirName = "\\\\/:*?\\\"<>|/:"

        TempFolder().use { folder ->
            assertFailsWith<TempFolderException> {
                folder.createDirectory(invalidDirName)
            }
        }
    }

    @Test
    fun `빈 파일명으로 createFile 하면 예외가 발생한다`() {
        TempFolder().use { folder ->
            assertFailsWith<IllegalArgumentException> {
                folder.createFile("")
            }
            assertFailsWith<IllegalArgumentException> {
                folder.createFile("   ")
            }
        }
    }

    @Test
    fun `빈 폴더명으로 createDirectory 하면 예외가 발생한다`() {
        TempFolder().use { folder ->
            assertFailsWith<IllegalArgumentException> {
                folder.createDirectory("")
            }
            assertFailsWith<IllegalArgumentException> {
                folder.createDirectory("   ")
            }
        }
    }

    @Test
    fun `인자 없는 createFile 은 매번 다른 파일을 생성한다`() {
        TempFolder().use { folder ->
            val file1 = folder.createFile()
            val file2 = folder.createFile()

            file1.exists().shouldBeTrue()
            file2.exists().shouldBeTrue()
            (file1.name == file2.name).shouldBeFalse()
        }
    }

    @Test
    fun `rootPath 는 root 의 경로 문자열과 동일하다`() {
        TempFolder().use { folder ->
            folder.rootPath shouldBeEqualTo folder.root.path
        }
    }

    @Test
    fun `경로 순회 공격 시도 시 createFile 은 예외를 발생시킨다`() {
        TempFolder().use { folder ->
            assertFailsWith<IllegalArgumentException> {
                folder.createFile("../escape.txt")
            }
            assertFailsWith<IllegalArgumentException> {
                folder.createFile("../../etc/passwd")
            }
            assertFailsWith<IllegalArgumentException> {
                folder.createFile(folder.root.resolve("absolute.txt").absolutePath)
            }
        }
    }

    @Test
    fun `경로 순회 공격 시도 시 createDirectory 는 예외를 발생시킨다`() {
        TempFolder().use { folder ->
            assertFailsWith<IllegalArgumentException> {
                folder.createDirectory("../escape")
            }
            assertFailsWith<IllegalArgumentException> {
                folder.createDirectory("../../tmp")
            }
            assertFailsWith<IllegalArgumentException> {
                folder.createDirectory(folder.root.resolve("absolute").absolutePath)
            }
        }
    }

    @Test
    fun `루트 밖을 가리키는 symlink 부모 아래에는 파일을 생성할 수 없다`() {
        TempFolder().use { folder ->
            TempFolder().use { outside ->
                Files.createSymbolicLink(
                    folder.root.toPath().resolve("outside-link"),
                    outside.root.toPath()
                )

                assertFailsWith<IllegalArgumentException> {
                    folder.createFile("outside-link/escape.txt")
                }
            }
        }
    }

    @Test
    fun `루트 밖을 가리키는 symlink 부모 아래에는 디렉터리를 생성할 수 없다`() {
        TempFolder().use { folder ->
            TempFolder().use { outside ->
                Files.createSymbolicLink(
                    folder.root.toPath().resolve("outside-link"),
                    outside.root.toPath()
                )

                assertFailsWith<IllegalArgumentException> {
                    folder.createDirectory("outside-link/escape")
                }
            }
        }
    }

    @Test
    fun `MultithreadingTester 로 동시 파일 생성을 검증한다`() {
        TempFolder().use { folder ->
            val count = AtomicInteger()

            MultithreadingTester()
                .workers(4)
                .rounds(20)
                .add {
                    val index = count.incrementAndGet()
                    folder.createFile("thread-$index.txt").exists().shouldBeTrue()
                }
                .run()

            count.get() shouldBeEqualTo 80
        }
    }

    @Test
    fun `StructuredTaskScopeTester 로 구조화된 동시 디렉터리 생성을 검증한다`() {
        TempFolder().use { folder ->
            val count = AtomicInteger()

            StructuredTaskScopeTester()
                .rounds(40)
                .add {
                    val index = count.incrementAndGet()
                    folder.createDirectory("scope-$index").exists().shouldBeTrue()
                }
                .run()

            count.get() shouldBeEqualTo 40
        }
    }

    @Test
    fun `SuspendedJobTester 로 suspend 동시 파일 생성을 검증한다`() = runTest {
        TempFolder().use { folder ->
            val count = AtomicInteger()

            SuspendedJobTester()
                .workers(4)
                .rounds(40)
                .add {
                    val index = count.incrementAndGet()
                    folder.createFile("job-$index.txt").exists().shouldBeTrue()
                }
                .run()

            count.get() shouldBeEqualTo 40
        }
    }

    @Test
    fun `close 후 재호출해도 예외가 발생하지 않는다`() {
        val tempFolder = TempFolder()
        tempFolder.root.exists().shouldBeTrue()

        tempFolder.close()
        tempFolder.root.exists().shouldBeFalse()

        // 이미 삭제된 상태에서 재호출해도 예외 없이 종료
        tempFolder.close()
    }
}
