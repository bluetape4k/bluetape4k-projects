package io.bluetape4k.io

import java.io.File
import java.io.IOException
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Result 패턴을 사용하는 파일 유틸리티 함수들
 *
 * 기존 nullable 반환 함수들의 안전한 대안을 제공합니다.
 * `tryXXXX` 패턴으로 명명하여 Result<T>를 반환함을 명시합니다.
 */

/**
 * 디렉토리를 생성하고 Result로 반환합니다.
 *
 * @param path 생성할 디렉토리 경로
 * @return 생성된 디렉토리의 Result. 실패 시 예외 정보 포함
 *
 * @sample
 * ```kotlin
 * tryCreateDirectory("/tmp/mydir").fold(
 *     onSuccess = { dir -> println("Created: ${dir.absolutePath}") },
 *     onFailure = { error -> logger.error("Failed", error) }
 * )
 * ```
 */
fun tryCreateDirectory(path: String): Result<File> = runCatching {
    val file = File(path)

    // 이미 존재하는 디렉토리면 그대로 반환
    if (file.exists() && file.isDirectory) {
        return@runCatching file
    }

    // 파일로 존재하는 경우 에러
    if (file.exists() && file.isFile) {
        throw IOException("Path exists as file, not directory: $path")
    }

    // 디렉토리 생성
    if (!file.mkdirs()) {
        throw IOException("Failed to create directory: $path")
    }

    file
}

/**
 * 빈 파일을 생성하고 Result로 반환합니다.
 *
 * @param path 생성할 파일 경로
 * @return 생성된 파일의 Result
 */
fun tryCreateFile(path: String): Result<File> = runCatching {
    val file = File(path)

    // 이미 존재하는 파일이면 그대로 반환
    if (file.exists() && file.isFile) {
        return@runCatching file
    }

    // 디렉토리로 존재하는 경우 에러
    if (file.exists() && file.isDirectory) {
        throw IOException("Path exists as directory, not file: $path")
    }

    // 부모 디렉토리가 없으면 생성
    file.parentFile?.let { parent ->
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Failed to create parent directory: ${parent.absolutePath}")
        }
    }

    // 파일 생성
    if (!file.createNewFile()) {
        throw IOException("Failed to create file: $path")
    }

    file
}

/**
 * 디렉토리를 재귀적으로 삭제하고 Result로 반환합니다.
 *
 * @receiver 삭제할 디렉토리
 * @return 삭제 성공 여부의 Result
 */
fun File.tryDeleteRecursively(): Result<Boolean> = runCatching {
    if (!exists()) {
        return@runCatching true
    }

    if (!isDirectory) {
        throw IOException("Not a directory: $absolutePath")
    }

    deleteRecursively()
}

/**
 * 파일이 존재하면 삭제하고 Result로 반환합니다.
 *
 * @receiver 삭제할 파일
 * @return 삭제 성공 여부의 Result (파일이 없어도 성공)
 */
fun File.tryDeleteIfExists(): Result<Boolean> = runCatching {
    if (!exists()) {
        true
    } else {
        delete()
    }
}

// ========================================
// Path 확장 함수들
// ========================================

/**
 * 파일의 모든 바이트를 읽어 Result로 반환합니다.
 *
 * @receiver 읽을 파일 경로
 * @return 파일 내용의 Result
 */
fun Path.tryReadAllBytes(): Result<ByteArray> = runCatching {
    if (!exists()) {
        throw IOException("File not found: $this")
    }

    if (isDirectory()) {
        throw IOException("Path is a directory: $this")
    }

    Files.readAllBytes(this)
}

/**
 * 파일에 바이트를 쓰고 Result로 반환합니다.
 *
 * @receiver 쓸 파일 경로
 * @param bytes 쓸 바이트 배열
 * @return 쓰여진 바이트 수의 Result
 */
fun Path.tryWriteBytes(bytes: ByteArray): Result<Long> = runCatching {
    // 부모 디렉토리 생성
    parent?.let { parentDir ->
        if (!parentDir.exists()) {
            Files.createDirectories(parentDir)
        }
    }

    Files.write(this, bytes)
    bytes.size.toLong()
}

/**
 * 파일에 라인들을 쓰고 Result로 반환합니다.
 *
 * @receiver 쓸 파일 경로
 * @param lines 쓸 라인들
 * @param charset 문자 인코딩 (기본: UTF-8)
 * @return 쓰여진 바이트 수의 Result
 */
fun Path.tryWriteLines(
    lines: Iterable<String>,
    charset: java.nio.charset.Charset = Charsets.UTF_8,
): Result<Long> = runCatching {
    // 부모 디렉토리 생성
    parent?.let { parentDir ->
        if (!parentDir.exists()) {
            Files.createDirectories(parentDir)
        }
    }

    Files.write(this, lines, charset)
    Files.size(this)
}

/**
 * 파일의 모든 라인을 읽어 Result로 반환합니다.
 *
 * @receiver 읽을 파일 경로
 * @param charset 문자 인코딩 (기본: UTF-8)
 * @return 라인 리스트의 Result
 */
fun Path.tryReadAllLines(charset: java.nio.charset.Charset = Charsets.UTF_8): Result<List<String>> = runCatching {
    if (!exists()) {
        throw IOException("File not found: $this")
    }

    if (isDirectory()) {
        throw IOException("Path is a directory: $this")
    }

    Files.readAllLines(this, charset)
}

// ========================================
// 비동기 파일 작업 (Result 버전)
// ========================================

/**
 * 파일을 비동기로 복사하고 CompletableFuture<Result<File>>로 반환합니다.
 *
 * @receiver 원본 파일
 * @param target 대상 파일
 * @param overwrite 덮어쓰기 여부 (기본: false)
 * @return 복사된 파일의 Result를 담은 CompletableFuture
 */
fun File.tryCopyToAsync(target: File, overwrite: Boolean = false): CompletableFuture<Result<File>> {
    return CompletableFuture.supplyAsync({
        runCatching {
            if (!exists()) {
                throw IOException("Source file not found: $absolutePath")
            }

            if (!isFile) {
                throw IOException("Source is not a file: $absolutePath")
            }

            if (target.exists() && !overwrite) {
                throw IOException("Target file already exists: ${target.absolutePath}")
            }

            // 대상 디렉토리 생성
            target.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    throw IOException("Failed to create target directory: ${parent.absolutePath}")
                }
            }

            copyTo(target, overwrite)
        }
    }, defaultFileExecutor)
}

/**
 * 파일을 비동기로 이동하고 CompletableFuture<Result<File>>로 반환합니다.
 *
 * @receiver 원본 파일
 * @param target 대상 파일
 * @param overwrite 덮어쓰기 여부 (기본: false)
 * @return 이동된 파일의 Result를 담은 CompletableFuture
 */
fun File.tryMoveAsync(target: File, overwrite: Boolean = false): CompletableFuture<Result<File>> {
    return CompletableFuture.supplyAsync({
        runCatching {
            if (!exists()) {
                throw IOException("Source file not found: $absolutePath")
            }

            if (target.exists() && !overwrite) {
                throw IOException("Target file already exists: ${target.absolutePath}")
            }

            // 대상 디렉토리 생성
            target.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    throw IOException("Failed to create target directory: ${parent.absolutePath}")
                }
            }

            // 이동 시도 (같은 파일시스템이면 rename, 아니면 copy & delete)
            if (!renameTo(target)) {
                copyTo(target, overwrite)
                if (!delete()) {
                    throw IOException("Failed to delete source file after copy: $absolutePath")
                }
            }

            target
        }
    }, defaultFileExecutor)
}

/**
 * Path에서 비동기로 모든 바이트를 읽고 CompletableFuture<Result<ByteArray>>로 반환합니다.
 *
 * @receiver 읽을 파일 경로
 * @return 파일 내용의 Result를 담은 CompletableFuture
 */
fun Path.tryReadAllBytesAsync(): CompletableFuture<Result<ByteArray>> {
    return CompletableFuture.supplyAsync({
        runCatching {
            if (!exists()) {
                throw IOException("File not found: $this")
            }

            if (isDirectory()) {
                throw IOException("Path is a directory: $this")
            }

            val channel = AsynchronousFileChannel.open(this, StandardOpenOption.READ)
            try {
                val size = Files.size(this)
                val buffer = java.nio.ByteBuffer.allocate(size.toInt())

                val bytesRead = channel.read(buffer, 0).get()
                if (bytesRead != size.toInt()) {
                    throw IOException("Failed to read entire file. Expected: $size, Read: $bytesRead")
                }

                buffer.array()
            } finally {
                channel.close()
            }
        }
    }, defaultFileExecutor)
}

/**
 * Path에 비동기로 바이트를 쓰고 CompletableFuture<Result<Long>>로 반환합니다.
 *
 * @receiver 쓸 파일 경로
 * @param bytes 쓸 바이트 배열
 * @return 쓰여진 바이트 수의 Result를 담은 CompletableFuture
 */
fun Path.tryWriteAsync(bytes: ByteArray): CompletableFuture<Result<Long>> {
    return CompletableFuture.supplyAsync({
        runCatching {
            // 부모 디렉토리 생성
            parent?.let { parentDir ->
                if (!parentDir.exists()) {
                    Files.createDirectories(parentDir)
                }
            }

            val channel = AsynchronousFileChannel.open(
                this,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            )

            try {
                val buffer = java.nio.ByteBuffer.wrap(bytes)
                val bytesWritten = channel.write(buffer, 0).get()
                bytesWritten.toLong()
            } finally {
                channel.close()
            }
        }
    }, defaultFileExecutor)
}

/**
 * Path에 비동기로 라인들을 쓰고 CompletableFuture<Result<Long>>로 반환합니다.
 *
 * @receiver 쓸 파일 경로
 * @param lines 쓸 라인들
 * @param charset 문자 인코딩 (기본: UTF-8)
 * @return 쓰여진 바이트 수의 Result를 담은 CompletableFuture
 */
fun Path.tryWriteLinesAsync(
    lines: Iterable<String>,
    charset: java.nio.charset.Charset = Charsets.UTF_8,
): CompletableFuture<Result<Long>> {
    return CompletableFuture.supplyAsync({
        runCatching {
            // 부모 디렉토리 생성
            parent?.let { parentDir ->
                if (!parentDir.exists()) {
                    Files.createDirectories(parentDir)
                }
            }

            val text = lines.joinToString(System.lineSeparator())
            val bytes = text.toByteArray(charset)

            val channel = AsynchronousFileChannel.open(
                this,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            )

            try {
                val buffer = java.nio.ByteBuffer.wrap(bytes)
                channel.write(buffer, 0).get().toLong()
            } finally {
                channel.close()
            }
        }
    }, defaultFileExecutor)
}
