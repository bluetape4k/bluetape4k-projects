package io.bluetape4k.junit5.tempfolder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Files
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TempFolderExtensionParameterTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    private val tempFileNames = mutableSetOf<String>()
    private val tempDirNames = mutableSetOf<String>()

    @AfterAll
    fun alterAll() {
        tempFileNames.filter { Files.exists(Paths.get(it)) }.shouldBeEmpty()
        tempDirNames.filter { Files.exists(Paths.get(it)) }.shouldBeEmpty()
    }

    @Test
    @ExtendWith(TempFolderExtension::class)
    fun `인자로 temporary folder를 받을 수 있다`(tempFolder: TempFolder) {
        tempFolder.root.exists().shouldBeTrue()
        tempFolder.createFile("foo.txt").exists().shouldBeTrue()
        tempFolder.createDirectory("dir").exists().shouldBeTrue()
    }

    @Test
    @TempFolderTest
    fun `새로 생성된 임시 폴더의 부모 폴더는 root 폴더입니다`(tempFolder: TempFolder) {
        val root = tempFolder.root
        root.exists().shouldBeTrue()

        val dir = tempFolder.createDirectory("bar")
        dir.parentFile shouldBeEqualTo root
    }


    @RepeatedTest(REPEAT_SIZE)
    @ExtendWith(TempFolderExtension::class)
    fun `반복 수행되는 메소드에 대해 매번 temporary file이 생성됩니다`(tempFolder: TempFolder) {
        val file = tempFolder.createFile("foo.txt")
        log.trace { "임시파일=${file.absolutePath}" }
        file.exists().shouldBeTrue()

        tempFileNames shouldNotContain file.absolutePath
        tempFileNames.add(file.absolutePath)
    }

    @RepeatedTest(REPEAT_SIZE)
    @TempFolderTest
    fun `반복 수행되는 메소드에 대해 매번 temporary folder가 생성됩니다`(tempFolder: TempFolder) {
        val dir = tempFolder.createDirectory("bar")
        log.trace { "임시폴더=${dir.absolutePath}" }
        dir.exists().shouldBeTrue()

        tempDirNames shouldNotContain dir.absolutePath
        tempDirNames.add(dir.absolutePath)
    }
}
