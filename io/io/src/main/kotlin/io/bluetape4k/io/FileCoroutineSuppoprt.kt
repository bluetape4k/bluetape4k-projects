package io.bluetape4k.io

import kotlinx.coroutines.future.await
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path

/**
 * Coroutine 방식으로 [Path]의 모든 바이트를 읽어옵니다.
 *
 * ```
 * runBlocking {
 *      val path = Paths.get("path/to/file")
 *      val bytes = path.readAllBytesSuspending()
 * }
 * ```
 *
 * @return ByteArray 파일의 모든 바이트
 */
suspend inline fun Path.readAllBytesSuspending(): ByteArray =
    readAllBytesAsync().await()

/**
 * Coroutine 방식으로 [File]의 모든 바이트를 읽어옵니다.
 *
 * ```
 * runBlocking {
 *     val file = File("path/to/file")
 *     val bytes = file.suspendReadAllBytes()
 *     println(bytes.size)
 * }
 *
 * @return ByteArray 파일의 모든 바이트
 */
suspend inline fun File.readAllBytesSuspending(): ByteArray = toPath().readAllBytesSuspending()

@Deprecated("Use readAllBytesSuspending() instead", ReplaceWith("readAllBytesSuspending()"))
suspend fun Path.suspendReadAllBytes(): ByteArray = readAllBytesSuspending()

@Deprecated("Use readAllBytesSuspending() instead", ReplaceWith("readAllBytesSuspending()"))
suspend fun File.suspendReadAllBytes(): ByteArray = readAllBytesSuspending()

/**
 * Corutine 방식으로 [Path]의 모든 라인을 읽어옵니다.
 *
 * ```
 * runBlocking {
 *    val path = Paths.get("path/to/file")
 *    val lines = path.suspendReadAllLines()
 *    lines.forEach { println(it) }
 * }
 * ```
 *
 * @param charset Charset 파일의 인코딩 (기본값: UTF-8)
 * @return 파일의 모든 라인
 */
suspend fun Path.readAllLinesSuspending(charset: Charset = Charsets.UTF_8): List<String> {
    return readAllBytesSuspending()
        .toString(charset)
        .lineSequence()
        .toList()
}

@Deprecated("Use readAllLinesSuspending() instead", ReplaceWith("readAllLinesSuspending()"))
suspend fun Path.suspendReadAllLines(charset: Charset = Charsets.UTF_8): List<String> =
    readAllLinesSuspending(charset)

/**
 * I/O 처리에서 `suspendWrite` 함수를 제공합니다.
 */
suspend inline fun File.writeSuspending(bytes: ByteArray, append: Boolean = false): Long =
    toPath().writeAsync(bytes, append).await()


@Deprecated("Use writeSuspending() instead", ReplaceWith("writeSuspending(bytes, append)"))
suspend fun File.suspendWrite(bytes: ByteArray, append: Boolean = false): Long {
    return toPath().writeAsync(bytes, append).await()
}

/**
 * [Path]에 [bytes]를 비동기 방식으로 쓰기합니다.
 *
 * ```
 * runBlocking {
 *    val path = Paths.get("path/to/file")
 *    val bytes = "Hello, World!".toByteArray()
 *    path.suspendWrite(bytes)
 *    path.suspendReadAllBytes().toString(Charsets.UTF_8) shouldBeEqualTo "Hello, World!"
 * }
 *
 * @param bytes ByteArray 파일에 쓸 내용
 * @param append Boolean  추가 여부
 * @return Long 파일에 쓴 바이트 수
 */
suspend fun Path.writeSuspending(bytes: ByteArray, append: Boolean = false): Long =
    writeAsync(bytes, append).await()

@Deprecated("Use writeSuspending() instead", ReplaceWith("writeSuspending(bytes, append)"))
suspend fun Path.suspendWrite(bytes: ByteArray, append: Boolean = false): Long =
    writeSuspending(bytes, append)

/**
 * I/O 처리에서 `suspendWriteLines` 함수를 제공합니다.
 */
suspend inline fun File.writeLinesSuspending(
    lines: Iterable<String>,
    append: Boolean = false,
    charset: Charset = Charsets.UTF_8,
): Long =
    toPath().writeLinesAsync(lines, append, charset).await()

@Deprecated("Use writeLinesSuspending() instead", ReplaceWith("writeLinesSuspending(lines, append, charset)"))
suspend fun File.suspendWriteLines(
    lines: Iterable<String>,
    append: Boolean = false,
    charset: Charset = Charsets.UTF_8,
): Long {
    return toPath().writeLinesAsync(lines, append, charset).await()
}

/**
 * [Path]에 [Iterable]의 라인을 비동기 방식으로 쓰기합니다.
 *
 * ```
 * runBlocking {
 *      val path = Paths.get("path/to/file")
 *      val lines = listOf("Hello, World!", "안녕하세요, 세계!")
 *      path.suspendWriteLines(lines)
 * }
 * ```
 *
 * @param lines Iterable<String> 파일에 쓸 라인
 * @param append Boolean 추가 여부
 * @param charset 파일의 인코딩 (기본값: UTF-8)
 */
suspend inline fun Path.writeLinesSuspending(
    lines: Iterable<String>,
    append: Boolean = false,
    charset: Charset = Charsets.UTF_8,
): Long =
    writeLinesAsync(lines, append, charset).await()

@Deprecated("Use writeLinesSuspending() instead", ReplaceWith("writeLinesSuspending(lines, append, charset)"))
suspend fun Path.suspendWriteLines(
    lines: Iterable<String>,
    append: Boolean = false,
    charset: Charset = Charsets.UTF_8,
): Long {
    return writeLinesAsync(lines, append, charset).await()
}
