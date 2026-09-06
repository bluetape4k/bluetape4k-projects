package io.bluetape4k.io.readme

import io.bluetape4k.io.writeAtomically
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class AtomicFileReadmeKotlinContractTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `bounded copy example은 한도 초과 target을 commit하지 않는다`() {
        val source = tempDir.resolve("oversized-input.bin")
        val destination = tempDir.resolve("bounded-output.bin")
        val maxPayloadBytes = 8L * 1024
        Files.write(source, ByteArray(maxPayloadBytes.toInt() + 1) { 0x2A })

        assertThrows<IllegalArgumentException> {
            destination.writeAtomically { output ->
                Files.newInputStream(source).use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        require(copied + read <= maxPayloadBytes) { "payload exceeds configured limit" }
                        output.write(buffer, 0, read)
                        copied += read
                    }
                }
            }
        }

        assertFalse(Files.exists(destination))
    }

    @Test
    fun `coroutine example은 dispatcher와 cancellation 검사를 호출자가 소유한다`() = runBlocking {
        val source = tempDir.resolve("coroutine-input.bin")
        val destination = tempDir.resolve("coroutine-output.bin")
        val payload = "coroutine-content".toByteArray()
        Files.write(source, payload)

        assertEquals(payload.size.toLong(), copyAtomically(source, destination))
        assertArrayEquals(payload, Files.readAllBytes(destination))
    }

    private suspend fun copyAtomically(source: Path, destination: Path): Long {
        val callerContext = currentCoroutineContext()
        return withContext(Dispatchers.IO) {
            destination.writeAtomically { output ->
                Files.newInputStream(source).use { input -> input.copyTo(output) }
                callerContext.ensureActive()
            }
        }
    }
}
