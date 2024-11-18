package io.bluetape4k.junit5.tempfolder

import io.bluetape4k.logging.KLogging
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
}
