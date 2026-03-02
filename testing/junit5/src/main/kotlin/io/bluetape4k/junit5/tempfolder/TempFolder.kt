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
 * 테스트용 임시 루트 디렉터리와 하위 파일/디렉터리 생성을 관리합니다.
 *
 * ## 동작/계약
 * - 생성 시 즉시 OS 임시 디렉터리 아래에 루트 폴더를 하나 만듭니다.
 * - 파일/디렉터리 생성 실패는 [TempFolderException]으로 감싸 던집니다.
 * - [close] 호출 시 루트 경로를 재귀 삭제하며 실패는 경고 로그로만 남깁니다.
 *
 * ```kotlin
 * TempFolder().use { tf ->
 *   val f = tf.createFile("a.txt")
 *   // f.exists() == true
 * }
 * ```
 */
class TempFolder: Closeable {

    companion object: KLogging() {
        private const val PREFIX = "bluetape4k_"
    }

    private val rootFile: File

    /** 임시 루트 디렉터리 객체입니다. */
    val root: File get() = rootFile

    /** 임시 루트 디렉터리 경로 문자열입니다. */
    val rootPath: String get() = rootFile.path

    init {
        try {
            rootFile = Files.createTempDirectory(PREFIX).toFile()
            log.debug { "임시 폴더를 생성했습니다. [${rootFile.path}]" }
        } catch (e: IOException) {
            throw TempFolderException("임시폴더를 생성하는데 실패했습니다.", e)
        }
    }

    /**
     * 임시 루트 아래에 무작위 이름 파일을 생성합니다.
     *
     * ## 동작/계약
     * - 루트 경로 하위에 prefix 기반 임시 파일을 생성합니다.
     * - I/O 실패 시 [TempFolderException]이 발생합니다.
     */
    fun createFile(): File {
        return try {
            Files.createTempFile(rootFile.toPath(), PREFIX, null).toFile().apply {
                log.debug { "임시 파일을 생성했습니다. file=[$this]" }
            }
        } catch (e: IOException) {
            throw TempFolderException("임시 파일을 생성하는데 실패했습니다.", e)
        }
    }

    /**
     * 임시 루트 아래에 지정한 이름의 파일을 생성합니다.
     *
     * ## 동작/계약
     * - `filename`이 blank이면 [IllegalArgumentException]이 발생합니다.
     * - 이미 파일이 존재하거나 생성 실패 시 [TempFolderException]이 발생합니다.
     *
     * @param filename 생성할 파일 이름(공백 불가)
     */
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

    /**
     * 임시 루트 아래에 지정한 이름의 디렉터리를 생성합니다.
     *
     * ## 동작/계약
     * - `dir`이 blank이면 [IllegalArgumentException]이 발생합니다.
     * - 생성 실패 시 [TempFolderException]이 발생합니다.
     *
     * @param dir 생성할 디렉터리 이름(공백 불가)
     */
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

    /**
     * 임시 루트를 정리합니다.
     *
     * ## 동작/계약
     * - 내부 `destroy()` 실패 예외는 삼키고 경고 로그만 남깁니다.
     * - `use {}` 패턴에서 자동 호출됩니다.
     */
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
