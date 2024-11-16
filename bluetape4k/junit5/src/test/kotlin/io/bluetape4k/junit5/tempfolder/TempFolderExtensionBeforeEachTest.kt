package io.bluetape4k.junit5.tempfolder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Paths

@TempFolderTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TempFolderExtensionBeforeEachTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    private val tempFileNames = mutableSetOf<String>()
    private val tempDirNames = mutableSetOf<String>()

    private lateinit var tempFolder: TempFolder

    @BeforeEach
    fun beforeEach(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @AfterAll
    fun alterAll() {
        tempFileNames.filter { Files.exists(Paths.get(it)) }.shouldBeEmpty()
        tempDirNames.filter { Files.exists(Paths.get(it)) }.shouldBeEmpty()
    }

    @Test
    fun `메소드마다 새로운 temporary folder를 생성`() {
        tempFolder.createFile("foo.txt").exists().shouldBeTrue()
        tempFolder.createDirectory("bar").exists().shouldBeTrue()
    }

    @Test
    fun `새로운 폴더의 부모 폴더는 root 입니다`() {
        val root = tempFolder.root
        root.exists().shouldBeTrue()

        val dir = tempFolder.createDirectory("bar")
        dir.parentFile shouldBeEqualTo root
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `반복 수행되는 메소드에 대해 매번 temporary file이 생성됩니다`() {
        val file = tempFolder.createFile("foo.txt")
        log.trace { "임시파일=${file.absolutePath}" }
        file.exists().shouldBeTrue()

        tempFileNames shouldNotContain file.absolutePath
        tempFileNames.add(file.absolutePath)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `반복 수행되는 메소드에 대해 매번 temporary folder가 생성됩니다`() {
        val dir = tempFolder.createDirectory("bar")
        log.trace { "임시폴더=${dir.absolutePath}" }
        dir.exists().shouldBeTrue()

        tempDirNames shouldNotContain dir.absolutePath
        tempDirNames.add(dir.absolutePath)
    }
}
