package io.bluetape4k.io

import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

@TempFolderTest
class PathSupportTest {

    companion object: KLogging()

    private lateinit var tempFolder: TempFolder

    @BeforeEach
    fun setup(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @Test
    fun `remove file extension`() {
        val path: Path = Paths.get("/usr/local/var/filename.txt")

        path.removeFileExtension() shouldBeEqualTo "/usr/local/var/filename"
        path.fileName.removeFileExtension() shouldBeEqualTo "filename"
    }

    @Test
    fun `combine sub paths as string`() {
        val rootPath = Paths.get("/")
        val path = rootPath.combine("/usr", "/local", "/var", "/filename.txt")
        path.toString() shouldBeEqualTo "/usr/local/var/filename.txt"
    }

    @Test
    fun `combine sub paths`() {
        val rootPath = Paths.get("/")
        val path = rootPath.combine(
            Paths.get("/usr"), Paths.get("/local"), Paths.get("/var"), Paths.get("/filename.txt")
        )
        path.toString() shouldBeEqualTo "/usr/local/var/filename.txt"
    }

    @Test
    fun `relativize and normalize`() {
        Paths.get("/usr").normalize().toString() shouldBeEqualTo "/usr"
        Paths.get("/usr").normalizeAndRelativize().toString() shouldBeEqualTo "usr"
    }

    @Test
    fun `path exists operator`() {
        tempFolder.createFile().toPath().exists().shouldBeTrue()
        tempFolder.createFile("foo.txt").toPath().exists().shouldBeTrue()
    }
}
