package io.bluetape4k.io

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.CopyOption
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.CancellationException

class AtomicFileFailurePolicyTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `relative target은 absolute normalized parent로 해석한다`() {
        val requested = Path.of("build/atomic-file-support/../atomic-target.bin")
        val expected = IOException("stop after normalization")
        val operations = RecordingAtomicFileOperations().apply {
            createDirectoriesFailure = expected
        }

        val actual = assertThrows<IOException> {
            AtomicFileWriter(operations).write(requested) {}
        }

        assertSame(expected, actual)
        assertEquals(requested.toAbsolutePath().normalize().parent, operations.createdDirectory)
        assertEquals(0, operations.tempCalls)
    }

    @Test
    fun `move 전에 stream을 닫고 정확한 atomic option만 전달한다`() {
        val operations = RecordingAtomicFileOperations()
        val target = tempDir.resolve("nested/../success.bin")

        val written = AtomicFileWriter(operations).write(target) { output ->
            output.write(byteArrayOf(1, 2))
            output.write(3)
        }

        assertEquals(3L, written)
        assertTrue(operations.closedBeforeMove)
        assertEquals(
            listOf(StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING),
            operations.moveOptions,
        )
        assertEquals(target.toAbsolutePath().normalize(), operations.lastMoveTarget)
        assertEquals(0, operations.deleteCalls)
    }

    @Test
    fun `parent 생성 실패 뒤에는 후속 단계와 writer를 실행하지 않는다`() {
        val expected = IOException("parent")
        val operations = RecordingAtomicFileOperations().apply { createDirectoriesFailure = expected }
        var writerCalled = false

        val actual = assertThrows<IOException> {
            AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) { writerCalled = true }
        }

        assertSame(expected, actual)
        assertFalse(writerCalled)
        assertEquals(0, operations.tempCalls)
        assertEquals(0, operations.openCalls)
        assertEquals(0, operations.moveCalls)
    }

    @Test
    fun `temp 생성 실패 뒤에는 stream과 writer를 실행하지 않는다`() {
        val expected = IOException("temp")
        val operations = RecordingAtomicFileOperations().apply { createTempFailure = expected }
        var writerCalled = false

        val actual = assertThrows<IOException> {
            AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) { writerCalled = true }
        }

        assertSame(expected, actual)
        assertFalse(writerCalled)
        assertEquals(0, operations.openCalls)
        assertEquals(0, operations.moveCalls)
        assertEquals(0, operations.deleteCalls)
    }

    @Test
    fun `stream open 실패는 staged file을 정리하고 writer와 move를 실행하지 않는다`() {
        val expected = IOException("open")
        val operations = RecordingAtomicFileOperations().apply { openFailure = expected }
        var writerCalled = false

        val actual = assertThrows<IOException> {
            AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) { writerCalled = true }
        }

        assertSame(expected, actual)
        assertFalse(writerCalled)
        assertEquals(0, operations.moveCalls)
        assertEquals(1, operations.deleteCalls)
    }

    @Test
    fun `stream open의 unchecked failure identity를 보존하고 staged file을 정리한다`() {
        val expected = AssertionError("open")
        val operations = RecordingAtomicFileOperations().apply { openFailure = expected }

        val actual = assertThrows<AssertionError> {
            AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) {}
        }

        assertSame(expected, actual)
        assertEquals(0, operations.moveCalls)
        assertEquals(1, operations.deleteCalls)
    }

    @Test
    fun `callback close cleanup 실패의 identity와 suppressed 순서를 보존한다`() {
        val callback = IllegalStateException("callback")
        val close = IOException("close")
        val cleanup = IOException("cleanup")
        val operations = RecordingAtomicFileOperations().apply {
            closeFailure = close
            deleteFailure = cleanup
        }

        try {
            val actual = assertThrows<IllegalStateException> {
                AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) { throw callback }
            }

            assertSame(callback, actual)
            assertArrayEquals(arrayOf(close, cleanup), actual.suppressed)
        } finally {
            operations.lastTemp?.let(Files::deleteIfExists)
        }
    }

    @Test
    fun `callback 성공 뒤 close 실패는 primary가 되고 staged file을 정리한다`() {
        val close = IOException("close")
        val cleanup = IOException("cleanup")
        val target = tempDir.resolve("close.bin")
        Files.writeString(target, "old")
        val operations = RecordingAtomicFileOperations().apply {
            closeFailure = close
            deleteFailure = cleanup
        }

        try {
            val actual = assertThrows<IOException> {
                AtomicFileWriter(operations).write(target) {
                    it.write("complete".toByteArray())
                }
            }

            assertSame(close, actual)
            assertSame(cleanup, actual.suppressed.single())
            assertEquals("old", Files.readString(target))
            assertEquals(0, operations.moveCalls)
            assertEquals(1, operations.deleteCalls)
        } finally {
            operations.lastTemp?.let(Files::deleteIfExists)
        }
    }

    @Test
    fun `같은 failure instance는 suppressed에 중복 연결하지 않는다`() {
        val primary = IOException("same")
        val operations = RecordingAtomicFileOperations().apply {
            closeFailure = primary
            deleteFailure = primary
        }

        try {
            val actual = assertThrows<IOException> {
                AtomicFileWriter(operations).write(tempDir.resolve("same-failure.bin")) { throw primary }
            }

            assertSame(primary, actual)
            assertTrue(actual.suppressed.isEmpty())
        } finally {
            operations.lastTemp?.let(Files::deleteIfExists)
        }
    }

    @Test
    fun `cleanup graph가 primary를 가리키면 순환 suppressed를 만들지 않는다`() {
        val callback = IllegalStateException("callback")
        val cleanup = IOException("cleanup", callback)
        val operations = RecordingAtomicFileOperations().apply { deleteFailure = cleanup }

        try {
            val actual = assertThrows<IllegalStateException> {
                AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) { throw callback }
            }

            assertSame(callback, actual)
            assertTrue(actual.suppressed.isEmpty())
        } finally {
            operations.lastTemp?.let(Files::deleteIfExists)
        }
    }

    @Test
    fun `CancellationException과 Error primary identity를 유지한다`() {
        listOf<Throwable>(CancellationException("cancel"), AssertionError("fatal")).forEach { primary ->
            val cleanup = IOException("cleanup")
            val operations = RecordingAtomicFileOperations().apply { deleteFailure = cleanup }
            try {
                val actual = assertThrows<Throwable> {
                    AtomicFileWriter(operations).write(tempDir.resolve("${primary.javaClass.simpleName}.bin")) {
                        throw primary
                    }
                }
                assertSame(primary, actual)
                assertSame(cleanup, actual.suppressed.single())
            } finally {
                operations.lastTemp?.let(Files::deleteIfExists)
            }
        }
    }

    @Test
    fun `atomic move 거부는 fallback 없이 전파하고 기존 target을 보존한다`() {
        val target = tempDir.resolve("existing.bin")
        Files.writeString(target, "old")
        val failures = listOf<Throwable>(
            AtomicMoveNotSupportedException("staged", "target", "unsupported"),
            UnsupportedOperationException("unsupported option"),
            FileAlreadyExistsException(target.toString()),
            IOException("commit"),
        )

        failures.forEach { expected ->
            val operations = RecordingAtomicFileOperations().apply { moveFailure = expected }
            val actual = assertThrows<Throwable> {
                AtomicFileWriter(operations).write(target) { it.write("new".toByteArray()) }
            }

            assertSame(expected, actual)
            assertEquals("old", Files.readString(target))
            assertEquals(1, operations.moveCalls)
            assertEquals(1, operations.deleteCalls)
        }
    }

    private class RecordingAtomicFileOperations: AtomicFileOperations {
        var createDirectoriesFailure: Throwable? = null
        var createTempFailure: Throwable? = null
        var openFailure: Throwable? = null
        var closeFailure: Throwable? = null
        var moveFailure: Throwable? = null
        var deleteFailure: Throwable? = null
        var tempCalls = 0
        var openCalls = 0
        var moveCalls = 0
        var deleteCalls = 0
        var createdDirectory: Path? = null
        var lastTemp: Path? = null
        var lastMoveTarget: Path? = null
        var lastOutput: RecordingOutputStream? = null
        var closedBeforeMove = false
        var moveOptions: List<CopyOption> = emptyList()

        override fun createDirectories(directory: Path): Path {
            createdDirectory = directory
            createDirectoriesFailure?.let { throw it }
            return Files.createDirectories(directory)
        }

        override fun createTempFile(directory: Path, prefix: String, suffix: String): Path {
            tempCalls++
            createTempFailure?.let { throw it }
            return Files.createTempFile(directory, prefix, suffix).also { lastTemp = it }
        }

        override fun openOutput(path: Path): OutputStream {
            openCalls++
            openFailure?.let { throw it }
            return RecordingOutputStream(Files.newOutputStream(path)) { closeFailure }
                .also { lastOutput = it }
        }

        override fun move(source: Path, target: Path, vararg options: CopyOption): Path {
            moveCalls++
            closedBeforeMove = lastOutput?.closed == true
            lastMoveTarget = target
            moveOptions = options.toList()
            moveFailure?.let { throw it }
            return Files.move(source, target, *options)
        }

        override fun deleteIfExists(path: Path): Boolean {
            deleteCalls++
            deleteFailure?.let { throw it }
            return Files.deleteIfExists(path)
        }
    }

    private class RecordingOutputStream(
        delegate: OutputStream,
        private val failure: () -> Throwable?,
    ): FilterOutputStream(delegate) {
        var closed = false
            private set

        override fun close() {
            try {
                super.close()
            } finally {
                closed = true
            }
            failure()?.let { throw it }
        }
    }
}
