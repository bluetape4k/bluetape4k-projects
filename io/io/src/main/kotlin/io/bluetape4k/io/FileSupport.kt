package io.bluetape4k.io

import io.bluetape4k.concurrent.asCompletableFuture
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.support.LINE_SEPARATOR
import io.bluetape4k.support.closeSafe
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.utils.Runtimex
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.text.Charsets.UTF_8


private val log by lazy { KotlinLogging.logger {} }

const val EXTENSION_SEPARATOR = '.'
const val UNIX_SEPARATOR = '/'
const val WINDOW_SEPARATOR = '\\'

internal val defaultFileExecutor: ExecutorService by lazy {
    Executors.newVirtualThreadPerTaskExecutor().also { executor ->
        Runtimex.addShutdownHook {
            executor.shutdown()
        }
    }
}

@JvmField
val SYSTEM_SEPARATOR = File.separatorChar

/**
 * 디렉토리 생성, 실패 시에는 null을 반환합니다.
 *
 * ```
 * val dir = createDirectory("temp")
 * ```
 *
 * @param dir 생성할 디렉토리 경로
 * @return 생성된 디렉토리 [File], 실패 시 null
 */
fun createDirectory(dir: String): File? {
    log.trace { "Create directory. dir=[$dir]" }

    return try {
        val file = File(dir)
        val created = file.mkdirs()
        if (created || file.isDirectory) file else null
    } catch (e: Exception) {
        log.error(e) { "Fail to create directory. dir=[$dir]" }
        null
    }
}

/**
 * 파일 경로가 없을 때에는 파일의 경로에 해당하는 폴더들을 생성합니다.
 *
 * ```
 * val file = createFile("temp/sub/sub2")
 * file.createParentDirectory()
 * ```
 *
 * @receiver File 파일 경로
 * @throws IOException 파일 경로가 존재하지 않을 때
 */
fun File.createParentDirectory() {
    this.canonicalFile.parentFile?.run {
        mkdirs()
        if (!isDirectory) {
            throw IOException("Unable to create parent directory of ${this@createParentDirectory}")
        }
    }
}

/**
 * 파일을 생성합니다.
 *
 * ```
 * val file = createFile("temp/sub/temp.txt")
 * ```
 *
 * @param path 파일 경로
 * @return 생성된 파일 [File]
 */
fun createFile(path: String): File {
    log.trace { "Create file. path=[$path]" }

    val file = File(path)
    file.createParentDirectory()

    file.createNewFile()
    return file
}

/**
 * 임시 디렉토리를 생성합니다. [deleteAtExit]가 true라면 프로그램 종료 시 삭제할 수 있습니다.
 *
 * ```
 * val tempDir = createTempDirectory()
 * ```
 *
 * @param deleteAtExit Boolean 프로그램 종료 시 삭제할 것인가 여부
 * @return File 생성된 임시 디렉토리
 */
fun createTempDirectory(prefix: String = "temp", suffix: String = "dir", deleteAtExit: Boolean = true): File {
    val dir = File.createTempFile(prefix, suffix)
    dir.deleteRecursively()
    dir.mkdirs()

    if (deleteAtExit) {
        Runtimex.addShutdownHook {
            dir.deleteRecursively()
        }
    }
    return dir
}

/**
 * 파일을 비동기 방식으로 복사합니다.
 *
 * ```
 * val file = File("temp.txt")
 * val target = File("temp2.txt")
 * file.copyToAsync(target).get()
 * ```
 *
 * @param target 복사할 대상 파일
 * @param overwrite 덮어쓸 것인가 여부
 * @param bufferSize 버퍼 크기
 * @return 복사된 파일 [File]을 반환하는 [CompletableFuture]
 */
fun File.copyToAsync(
    target: File,
    overwrite: Boolean = false,
    bufferSize: Int = kotlin.io.DEFAULT_BUFFER_SIZE,
): CompletableFuture<File> =
    CompletableFuture.supplyAsync({
        this@copyToAsync.copyTo(target, overwrite, bufferSize)
    }, VirtualThreadExecutor)

/**
 * 파일을 이동합니다.
 *
 * ```
 * val file = File("temp.txt")
 * val target = File("temp2.txt")
 * file.move(target)
 * ```
 *
 * @param dest 이동할 대상 파일
 */
fun File.move(dest: File) {
    FileUtils.moveFile(this, dest)
}

/**
 * 파일을 비동기 방식으로 이동합니다.
 *
 * ```
 * val file = File("temp.txt")
 * val target = File("temp2.txt")
 * file.moveAsync(target).get()
 * ```
 *
 * @param dest 이동할 대상 파일
 * @return 이동된 파일 [File]을 반환하는 [CompletableFuture]
 */
fun File.moveAsync(dest: File): CompletableFuture<Void> =
    CompletableFuture.runAsync({ this@moveAsync.move(dest) }, VirtualThreadExecutor)

/**
 * 파일이 존재하면, 삭제합니다.
 */
fun File.deleteIfExists() {
    if (this.exists()) {
        FileUtils.deleteQuietly(this)
    }
}

/**
 * 디렉토리와 하위 디렉토리와 파일들을 모두 삭제합니다. 파일이라면 아무 일도 하지 않습니다.
 *
 * ```
 * val dir = File("temp")
 * dir.deleteDirectoryRecursively()
 * ```
 */
fun File.deleteDirectoryRecursively(): Boolean {
    if (isDirectory) {
        return deleteRecursively()
    }
    return false
}

/**
 * 디렉토리를 삭제합니다. [recusive]가 true라면 하위 디렉토리와 파일들을 모두 삭제합니다.
 *
 * ```
 * val dir = File("temp")
 * dir.deleteDirectory(recurse = true)
 * ```
 */
fun File.deleteDirectory(recusive: Boolean = true): Boolean {
    return if (recusive) {
        deleteDirectoryRecursively()
    } else {
        if (exists()) {
            FileUtils.deleteDirectory(this)
            true
        } else {
            false
        }
    }
}

/**
 * Unit "touch" utility를 구현한 함수입니다.
 * 파일이 존재하지 않는 경우 크기가 0 인 파일을 새로 만듭니다.
 *
 * ```
 * val file = File("temp.txt")
 * file.touch()
 * ```
 */
fun File.touch(): Boolean {
    if (!exists()) {
        FileOutputStream(this).closeSafe()
    }
    return setLastModified(System.currentTimeMillis())
}

/**
 * 파일을 읽어 [ByteArray]로 반환합니다.
 *
 * ```
 * val file = File("temp.txt")
 * val bytes = file.readAllBytes()
 * ```
 *
 * @return 파일을 내용을 담은[ByteArray]
 */
fun File.readAllBytes(): ByteArray {
    if (!exists()) {
        return emptyByteArray
    }
    return this.readBytes()
}

/**
 * 파일을 비동기 방식으로 읽어 [ByteArray]로 반환합니다.
 *
 * ```
 * val file = File("temp.txt")
 * val bytes = file.readAllBytesAsync().get()
 * ```
 *
 * @return 파일을 내용을 담은[ByteArray]를 반환하는 [CompletableFuture]
 */
fun Path.readAllBytesAsync(
    executor: ExecutorService = defaultFileExecutor,
): CompletableFuture<ByteArray> {
    val promise = CompletableFuture<ByteArray>()
    val channel = AsynchronousFileChannel.open(
        this,
        setOf(StandardOpenOption.READ),
        executor
    )
    try {
        val size = channel.size()
        require(size <= Int.MAX_VALUE) { "File is too large to read into a single ByteBuffer. size=$size" }
        val buffer = ByteBuffer.allocateDirect(size.toInt())

        fun readNext(position: Long) {
            channel.read(buffer, position).asCompletableFuture()
                .whenCompleteAsync({ read, error ->
                    if (error != null) {
                        channel.closeSafe()
                        promise.completeExceptionally(error)
                        return@whenCompleteAsync
                    }
                    if (read == null || read <= 0) {
                        try {
                            buffer.flip()
                            promise.complete(buffer.getBytes())
                        } finally {
                            channel.closeSafe()
                        }
                        return@whenCompleteAsync
                    }
                    readNext(position + read)
                }, executor)
        }

        if (buffer.capacity() == 0) {
            channel.closeSafe()
            promise.complete(emptyByteArray)
        } else {
            readNext(0L)
        }
    } catch (e: Throwable) {
        channel.closeSafe()
        promise.completeExceptionally(e)
    }

    return promise
}

/**
 * 파일의 내용을 라인 단위로 읽어 [Sequence]로 반환합니다.
 *
 * ```
 * val file = File("temp.txt")
 * val lines = file.readLineSequence()
 * ```
 *
 * @param cs Charset 기본값은 UTF-8
 */
fun File.readLineSequence(cs: Charset = UTF_8): Sequence<String> =
    FileInputStream(this).toLineSequence(cs)

/**
 * 파일의 내용을 라인 단위로 읽어 [List]로 반환합니다.
 *
 * ```
 * val file = File("temp.txt")
 * val lines = file.readAllLines()
 * ```
 *
 * @param cs Charset 기본값은 UTF-8
 */
fun File.readAllLines(cs: Charset = UTF_8): List<String> =
    FileInputStream(this).toStringList(cs)

/**
 * 파일의 내용을 비동기 방식으로 라인 단위로 읽어 [List]로 반환합니다.
 *
 * ```
 * val file = File("temp.txt")
 * val lines = file.readAllLinesAsync().get()
 * ```
 *
 * @param cs Charset 기본값은 UTF-8
 */
fun Path.readAllLinesAsync(cs: Charset = UTF_8): CompletableFuture<List<String>> =
    readAllBytesAsync().thenApplyAsync { it.toString(cs).lines() }

/**
 * 파일에 [bytes] 내용을 씁니다.
 *
 * ```
 * val file = File("temp.txt")
 * file.write(byteArrayOf(1, 2, 3))
 * ```
 *
 * @param bytes 쓸 [ByteArray]
 * @param append 기존 파일에 추가할 것인가 여부 (기본값은 false)
 */
fun File.write(bytes: ByteArray, append: Boolean = false) {
    FileUtils.writeByteArrayToFile(this, bytes, append)
}

/**
 * 파일에 [lines] 내용을 씁니다.
 *
 * ```
 * val file = File("temp.txt")
 * file.writeLines(listOf("Hello", "World"))
 * ```
 *
 * @param lines 쓸 라인
 * @param append 기존 파일에 추가할 것인가 여부 (기본값은 false)
 * @param cs Charset 기본값은 UTF-8
 */
fun File.writeLines(lines: Collection<String>, append: Boolean = false, cs: Charset = UTF_8) {
    FileUtils.writeLines(this, cs.name(), lines, append)
}

/**
 * 지정한 경로[Path]에 [bytes] 내용을 비동기 방식으로 쓰고, 쓰기 완료 후 쓰여진 바이트 수를 반환하는 [CompletableFuture]를 반환합니다.
 *
 * ```
 * val path = Paths.get("temp.txt")
 * path.writeAsync(byteArrayOf(1, 2, 3)).get()
 * ```
 *
 * @param bytes 파일에 쓸 [ByteArray]
 * @param append 기존 파일에 추가할 것인가 여부
 * @return 파일에 쓰여진 바이트 수를 반환하는 [CompletableFuture]
 */
fun Path.writeAsync(
    bytes: ByteArray,
    append: Boolean = false,
    executor: ExecutorService = defaultFileExecutor,
): CompletableFuture<Long> {
    val promise = CompletableFuture<Long>()

    val options = arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    val channel = AsynchronousFileChannel.open(
        this,
        options.toSet(),
        executor
    )

    val startPos = if (append) channel.size() else 0L
    val content = bytes.toByteBufferDirect()

    fun writeNext(position: Long) {
        if (!content.hasRemaining()) {
            channel.closeSafe()
            promise.complete((position - startPos))
            return
        }
        channel.write(content, position).asCompletableFuture()
            .whenCompleteAsync({ written, error ->
                if (error != null) {
                    channel.closeSafe()
                    promise.completeExceptionally(error)
                    return@whenCompleteAsync
                }
                if (written == null || written <= 0) {
                    channel.closeSafe()
                    promise.completeExceptionally(IOException("No bytes were written"))
                    return@whenCompleteAsync
                }
                writeNext(position + written)
            }, executor)
    }

    writeNext(startPos)
    return promise
}

/**
 * 지정한 경로[Path]에 [lines] 내용을 비동기 방식으로 쓰고, 쓰기 완료 후 쓰여진 바이트 수를 반환하는 [CompletableFuture]를 반환합니다.
 *
 * ```
 * val path = Paths.get("temp.txt")
 * path.writeLinesAsync(listOf("Hello", "World")).get()
 * ```
 *
 * @param lines 파일에 쓸 라인
 * @param append 기존 파일에 추가할 것인가 여부
 * @param cs Charset
 * @return 파일에 쓰여진 바이트 수를 반환하는 [CompletableFuture]
 */
fun Path.writeLinesAsync(
    lines: Iterable<String>,
    append: Boolean = false,
    cs: Charset = UTF_8,
): CompletableFuture<Long> {
    val bytes = lines.joinToString(LINE_SEPARATOR).toByteArray(cs)
    return writeAsync(bytes, append)
}

/**
 * Path의 파일 내용을 읽기 위해 [BufferedReader]를 빌드합니다.
 *
 * ```
 * val path = Paths.get("temp.txt")
 * val reader = path.bufferedReader()
 * ```
 *
 * @receiver Path 파일 경로
 * @param cs Charset 기본값은 UTF-8
 * @param bufferSize 버퍼 크기
 * @return [BufferedReader]
 */
fun Path.bufferedReader(
    cs: Charset = UTF_8,
    bufferSize: Int = kotlin.io.DEFAULT_BUFFER_SIZE,
): BufferedReader =
    InputStreamReader(FileInputStream(this.toFile()), cs).buffered(bufferSize)

/**
 * Path의 파일에 데이터를 쓰기 위한 [BufferedWriter]를 빌드합니다.
 *
 * ```
 * val path = Paths.get("temp.txt")
 * val writer = path.bufferedWriter()
 * ```
 *
 * @receiver Path 파일 경로
 * @param cs Charset 기본값은 UTF-8
 * @param bufferSize 버퍼 크기
 * @return [BufferedWriter]
 */
fun Path.bufferedWriter(
    cs: Charset = UTF_8,
    bufferSize: Int = kotlin.io.DEFAULT_BUFFER_SIZE,
): BufferedWriter =
    OutputStreamWriter(FileOutputStream(this.toFile()), cs).buffered(bufferSize)

/**
 * File을 읽어 [ByteArray] 로 빌드합니다.
 *
 * ```
 * val file = File("temp.txt")
 * val bytes = file.toByteArray()
 * ```
 *
 * @receiver File 파일
 * @return 파일을 내용을 담은 [ByteArray]
 */
fun File.toByteArray(): ByteArray {
    return FileInputStream(this).use { input ->
        input.readBytes()
    }
}
