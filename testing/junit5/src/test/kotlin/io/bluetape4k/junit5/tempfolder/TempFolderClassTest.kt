package io.bluetape4k.junit5.tempfolder

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

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
