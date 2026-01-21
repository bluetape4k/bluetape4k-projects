package io.bluetape4k.junit5.tempfolder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 테스트 시 사용할 임시 폴더와 임시 파일을 생성하고 관리해주는 클래스입니다.
 */
class TempFolder: Closeable {

    companion object: KLogging() {
        private const val PREFIX = "bluetape4k_"
    }

    private val rootFile: File

    val root: File get() = rootFile

    init {
        try {
            rootFile = Files.createTempDirectory(PREFIX).toFile()
            log.debug { "임시 폴더를 생성했습니다. [${rootFile.path}]" }
        } catch (e: IOException) {
            throw TempFolderException("임시폴더를 생성하는데 실패했습니다.", e)
        }
    }

    fun createFile(): File {
        return try {
            Files.createTempFile(rootFile.toPath(), PREFIX, null).toFile().apply {
                log.debug { "임시 파일을 생성했습니다. file=[$this]" }
            }
        } catch (e: IOException) {
            throw TempFolderException("임시 파일을 생성하는데 실패했습니다.", e)
        }
    }

    fun createFile(filename: String): File {
        require(filename.isNotBlank()) { "filename must not be blank" }

        return try {
            val path = Paths.get(rootFile.path, filename)
            Files.createFile(path).toFile().apply {
                log.debug { "임시 파일을 생성했습니다. file=[$this]" }
            }
        } catch (e: IOException) {
            throw TempFolderException("임시 파일을 생성하는데 실패했습니다. filename=$filename", e)
        }
    }

    fun createDirectory(dir: String): File {
        require(dir.isNotBlank()) { "dir must not be blank" }

        return try {
            val path = Paths.get(rootFile.path, dir)
            Files.createDirectory(path).toFile().apply {
                log.debug { "임시 폴더를 생성했습니다. dir=[$this]" }
            }
        } catch (e: IOException) {
            throw TempFolderException("임시 파일을 생성하는데 실패했습니다. dir=$dir", e)
        }
    }

    override fun close() {
        runCatching { destroy() }
            .onFailure {
                log.warn(it) { "임시 폴더를 삭제하는데 실패했습니다. [${rootFile.path}]" }
            }
    }

    private fun destroy() {
        if (!root.exists()) {
            return
        }

        log.debug { "임시 폴더를 삭제합니다. [${rootFile.path}]" }
        try {
            root.deleteRecursively()
        } catch (e: Throwable) {
            throw TempFolderException("임시 폴더를 삭제하는데 실패했습니다. [${rootFile.path}]", e)
        }
    }
}
