package io.bluetape4k.io

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class AtomicFileSupportTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `새 target을 교체하고 provider가 기록한 byte 수를 반환한다`() {
        val target = tempDir.resolve("nested/result.bin")
        val first = "hello".toByteArray()
        val second = " world".toByteArray()

        val written = target.writeAtomically { output ->
            output.write(first)
            output.write(second)
            output.write('!'.code)
        }

        assertEquals(first.size + second.size + 1L, written)
        assertArrayEquals("hello world!".toByteArray(), Files.readAllBytes(target))
        assertNoSiblingTemps(target)
    }

    @Test
    fun `default provider에서 기존 target을 완전한 새 payload로 교체한다`() {
        val target = tempDir.resolve("replace.bin")
        Files.writeString(target, "old payload")

        target.writeAtomically { it.write("new".toByteArray()) }

        assertEquals("new", Files.readString(target))
        assertNoSiblingTemps(target)
    }

    @Test
    fun `callback 실패는 같은 instance로 전파하고 기존 target과 cleanup을 보존한다`() {
        val target = tempDir.resolve("callback.bin")
        Files.writeString(target, "old")
        val expected = IllegalStateException("callback")

        val actual = assertThrows<IllegalStateException> {
            target.writeAtomically { output ->
                output.write("partial".toByteArray())
                throw expected
            }
        }

        assertSame(expected, actual)
        assertEquals("old", Files.readString(target))
        assertNoSiblingTemps(target)
    }

    @Test
    fun `빈 receiver와 filesystem root를 거부한다`() {
        assertThrows<IllegalArgumentException> { Path.of("").writeAtomically {} }
        assertThrows<IllegalArgumentException> { tempDir.root.writeAtomically {} }
    }

    @Test
    fun `후속 실패에서도 자동 생성한 parent는 남는다`() {
        val target = tempDir.resolve("created/parent/result.bin")

        assertThrows<IllegalArgumentException> {
            target.writeAtomically { throw IllegalArgumentException("reject") }
        }

        assertTrue(Files.isDirectory(target.parent))
        assertFalse(Files.exists(target))
        assertNoSiblingTemps(target)
    }

    @Test
    fun `서로 다른 target callback은 전역 직렬화 없이 동시에 진입한다`() {
        val entered = CountDownLatch(2)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val targets = listOf(tempDir.resolve("a.bin"), tempDir.resolve("b.bin"))
        val futures = targets.mapIndexed { index, target ->
            executor.submit<Long> {
                target.writeAtomically { output ->
                    entered.countDown()
                    assertTrue(release.await(5, TimeUnit.SECONDS))
                    output.write("payload-$index".toByteArray())
                }
            }
        }

        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            release.countDown()
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            release.countDown()
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }

        targets.forEach(::assertNoSiblingTemps)
    }

    @Test
    fun `같은 target의 동시 write는 완전한 payload 하나만 남긴다`() {
        val target = tempDir.resolve("same.bin")
        val payloads = listOf(ByteArray(4096) { 0x11 }, ByteArray(8192) { 0x22 })
        val staged = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)
        val futures = payloads.map { payload ->
            executor.submit<Long> {
                target.writeAtomically { output ->
                    output.write(payload)
                    staged.await(5, TimeUnit.SECONDS)
                }
            }
        }

        try {
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }

        val actual = Files.readAllBytes(target)
        assertTrue(payloads.any(actual::contentEquals))
        assertNoSiblingTemps(target)
    }

    @Test
    fun `bounded contention 중 reader는 완전한 payload만 관측하고 temp가 남지 않는다`() {
        val target = tempDir.resolve("contended.bin")
        val payloads = (1..4).map { marker -> ByteArray(32 * 1024) { marker.toByte() } }
        Files.write(target, payloads.first())
        val reading = AtomicBoolean(true)
        val partialSizes = ConcurrentLinkedQueue<Int>()
        val executor = Executors.newFixedThreadPool(payloads.size + 1)
        val reader = executor.submit<Unit> {
            while (reading.get()) {
                val actual = Files.readAllBytes(target)
                if (payloads.none(actual::contentEquals)) partialSizes.add(actual.size)
            }
        }
        val writers = payloads.map { payload ->
            executor.submit<Unit> {
                repeat(12) {
                    target.writeAtomically { output ->
                        var offset = 0
                        while (offset < payload.size) {
                            val length = minOf(1024, payload.size - offset)
                            output.write(payload, offset, length)
                            offset += length
                        }
                    }
                }
            }
        }

        try {
            writers.forEach { it.get(15, TimeUnit.SECONDS) }
            reading.set(false)
            reader.get(5, TimeUnit.SECONDS)
            assertTrue(partialSizes.isEmpty(), "partial sizes=$partialSizes")
        } finally {
            reading.set(false)
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }

        assertTrue(payloads.any(Files.readAllBytes(target)::contentEquals))
        assertNoSiblingTemps(target)
    }

    private fun assertNoSiblingTemps(target: Path) {
        val prefix = ".${target.fileName}."
        Files.list(target.parent).use { siblings ->
            assertFalse(siblings.anyMatch { path ->
                val name = path.fileName.toString()
                name.startsWith(prefix) && name.endsWith(".tmp")
            })
        }
    }
}
