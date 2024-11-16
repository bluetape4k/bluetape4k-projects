package io.bluetape4k.junit5.tempfolder

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files

@TempFolderTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TempFolderExtensionBeforeAllTest {

    private lateinit var tempFolder: TempFolder

    @BeforeAll
    fun beforeAll(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @AfterAll
    fun afterAll() {
        val createdFiles = Files.list(tempFolder.root.toPath()).map { it.toFile().name }.toList()
        createdFiles.size shouldBeEqualTo 2
        createdFiles shouldContainAll listOf("foo.txt", "bar")
    }

    @Test
    fun `임시 파일 생성`() {
        val file = tempFolder.createFile("foo.txt")
        file.exists().shouldBeTrue()
    }

    @Test
    fun `임시 디렉토리 생성`() {
        val dir = tempFolder.createDirectory("bar")
        dir.exists().shouldBeTrue()
    }
}
