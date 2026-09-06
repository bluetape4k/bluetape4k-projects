@file:JvmName("AtomicFileSupport")

package io.bluetape4k.io

import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap

/**
 * [writer]가 기록한 내용을 같은 parent의 temporary file에 먼저 쓴 뒤 atomic move로
 * receiver를 교체하고, provider에 성공적으로 기록한 byte 수를 반환합니다.
 *
 * 이 함수는 blocking API입니다. [writer]는 전달받은 stream을 callback 실행 중에만
 * 사용하고 직접 닫거나 보관하지 않아야 합니다. 별도 buffering wrapper는 callback이
 * 반환되기 전에 flush해야 합니다. callback과 stream close가 성공한 뒤에만
 * [StandardCopyOption.ATOMIC_MOVE]를 시도합니다.
 * Coroutine 호출자는 dispatcher를 직접 선택하고, callback 밖에서 캡처한 context의
 * cancellation을 callback이 반환되기 전에 검사해야 합니다.
 *
 * `ATOMIC_MOVE`가 적용되면 다른 move option은 무시될 수 있으므로 기존 target 교체는
 * filesystem provider 계약을 따릅니다. atomic move 거부에는 일반 move fallback을
 * 수행하지 않습니다. callback, close 또는 commit의 unchecked exception,
 * [java.util.concurrent.CancellationException], [Error]는 identity를 유지하며 cleanup
 * 실패만 primary에 suppressed로 연결됩니다.
 *
 * 이 API는 접근이 제한된 신뢰 가능한 parent에서만 사용해야 합니다. 경로 정규화는
 * lexical 처리일 뿐 path sandbox, symlink, hard-link, mount 교체와 TOCTOU를 방어하지
 * 않습니다. 공격자가 제어하는 공유 writable directory에는 사용하지 말고, 그 방어가
 * 필요하면 secure directory handle 기반 API를 선택해야 합니다. 임시 파일명에는 target
 * basename이 포함되고 permission은 provider default이므로, 민감한 payload에는 opaque
 * basename과 private parent를 사용해야 합니다.
 *
 * 기존 target의 권한, owner, ACL, xattr, fsync durability, payload 제한, logging과 crash
 * orphan cleanup은 보장하지 않습니다. 호출자는 target/provider와 primary/suppressed
 * 실패를 관측하되 전체 경로, basename, 예외 메시지의 민감한 값은 가려서 기록해야 합니다.
 *
 * @receiver 유효한 파일명을 가진 target 경로
 * @param writer provider가 소유하는 [OutputStream]에 내용을 기록하는 동기 callback
 * @return callback이 provider stream에 성공적으로 기록한 byte 수
 * @throws IllegalArgumentException receiver에 파일명이 없거나 비어 있을 때
 * @throws IOException parent, temporary file, stream 또는 atomic move provider 호출이 실패할 때
 * @throws Throwable writer, close 또는 commit이 던진 unchecked failure의 identity를 보존할 때
 */
@Throws(IOException::class)
fun Path.writeAtomically(writer: (OutputStream) -> Unit): Long =
    SystemAtomicFileWriter.write(this, writer)

private val SystemAtomicFileWriter = AtomicFileWriter(SystemAtomicFileOperations)

// Throwable identity와 suppressed 순서는 이 writer가 보존해야 하는 실패 계약이다.
@Suppress("TooGenericExceptionCaught")
internal class AtomicFileWriter(
    private val operations: AtomicFileOperations,
) {
    fun write(requestedTarget: Path, writer: (OutputStream) -> Unit): Long {
        val fileName = requestedTarget.fileName?.toString()
        require(!fileName.isNullOrEmpty()) { "Target path must have a file name" }

        val target = requestedTarget.toAbsolutePath().normalize()
        val parent = requireNotNull(target.parent) { "Normalized target must have a parent" }
        operations.createDirectories(parent)

        val staged = operations.createTempFile(parent, ".$fileName.", ".tmp")
        try {
            return writeStaged(staged, target, writer)
        } catch (failure: Throwable) {
            deleteTemporary(staged, failure)
            throw failure
        }
    }

    private fun writeStaged(staged: Path, target: Path, writer: (OutputStream) -> Unit): Long {
        val output = CountingOutputStream(operations.openOutput(staged))
        val callbackFailure = captureFailure { writer(output) }

        close(output, callbackFailure)?.let { throw it }
        val written = output.count
        operations.move(
            staged,
            target,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
        return written
    }

    private fun close(output: OutputStream, callbackFailure: Throwable?): Throwable? {
        try {
            output.close()
        } catch (closeFailure: Throwable) {
            if (callbackFailure == null) return closeFailure
            callbackFailure.attachSuppressedSafely(closeFailure)
        }
        return callbackFailure
    }

    private fun deleteTemporary(staged: Path, primaryFailure: Throwable) {
        try {
            operations.deleteIfExists(staged)
        } catch (cleanupFailure: Throwable) {
            primaryFailure.attachSuppressedSafely(cleanupFailure)
        }
    }
}

internal interface AtomicFileOperations {
    fun createDirectories(directory: Path): Path
    fun createTempFile(directory: Path, prefix: String, suffix: String): Path
    fun openOutput(path: Path): OutputStream
    fun move(source: Path, target: Path, vararg options: CopyOption): Path
    fun deleteIfExists(path: Path): Boolean
}

private object SystemAtomicFileOperations: AtomicFileOperations {
    override fun createDirectories(directory: Path): Path = Files.createDirectories(directory)

    override fun createTempFile(directory: Path, prefix: String, suffix: String): Path =
        Files.createTempFile(directory, prefix, suffix)

    override fun openOutput(path: Path): OutputStream = Files.newOutputStream(path)

    override fun move(source: Path, target: Path, vararg options: CopyOption): Path =
        Files.move(source, target, *options)

    override fun deleteIfExists(path: Path): Boolean = Files.deleteIfExists(path)
}

private class CountingOutputStream(delegate: OutputStream): FilterOutputStream(delegate) {
    var count: Long = 0L
        private set

    override fun write(value: Int) {
        out.write(value)
        count++
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        out.write(bytes, offset, length)
        count += length
    }
}

private fun Throwable.attachSuppressedSafely(secondary: Throwable) {
    val canAttach = this !== secondary &&
            !reaches(secondary) &&
            !secondary.reaches(this) &&
            suppressed.none { it === secondary }
    if (canAttach) addSuppressed(secondary)
}

@Suppress("TooGenericExceptionCaught")
private inline fun captureFailure(block: () -> Unit): Throwable? =
    try {
        block()
        null
    } catch (failure: Throwable) {
        failure
    }

private fun Throwable.reaches(target: Throwable): Boolean {
    val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    val pending = ArrayDeque<Throwable>()
    pending.add(this)

    while (pending.isNotEmpty()) {
        val current = pending.removeFirst()
        if (!visited.add(current)) continue
        if (current === target) return true
        current.cause?.let(pending::addLast)
        current.suppressed.forEach(pending::addLast)
    }
    return false
}
