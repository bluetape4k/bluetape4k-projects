package io.bluetape4k.io.serializer

import org.junit.jupiter.api.Assertions.fail
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal fun verifySerializerBufferConcurrency(
    operation: (worker: Int, repetition: Int) -> Unit,
) {
    val threadSequence = AtomicInteger()
    val executor = Executors.newFixedThreadPool(WORKERS) { task ->
        Thread(task, "serializer-buffer-${threadSequence.incrementAndGet()}")
    }
    val startBarrier = CyclicBarrier(WORKERS + 1)
    val completion = CountDownLatch(WORKERS)
    val unfinished = ConcurrentHashMap.newKeySet<Int>()
    val failures = ConcurrentLinkedQueue<String>()

    try {
        repeat(WORKERS) { worker ->
            unfinished += worker
            executor.submit {
                try {
                    startBarrier.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    repeat(REPETITIONS) { repetition ->
                        operation(worker, repetition)
                    }
                } catch (failure: Throwable) {
                    failures += "worker=$worker ${failure::class.java.simpleName}: ${failure.message}"
                } finally {
                    unfinished -= worker
                    completion.countDown()
                }
            }
        }

        startBarrier.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!completion.await(COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            fail<Unit>("Serializer buffer workers timed out; unfinished=${unfinished.sorted().take(MAX_DIAGNOSTICS)}")
        }
        if (failures.isNotEmpty()) {
            fail<Unit>("Serializer buffer worker failures: ${failures.take(MAX_DIAGNOSTICS)}")
        }
    } finally {
        executor.shutdownNow()
        if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            val threadNames =
                Thread.getAllStackTraces().keys.asSequence()
                    .filter { it.name.startsWith("serializer-buffer-") }
                    .map { it.name }
                    .take(MAX_DIAGNOSTICS)
                    .toList()
            fail<Unit>(
                "Serializer buffer executor did not terminate; " +
                    "unfinished=${unfinished.sorted().take(MAX_DIAGNOSTICS)}, " +
                    "threads=$threadNames",
            )
        }
    }
}

private const val WORKERS = 8
private const val REPETITIONS = 50
private const val START_TIMEOUT_SECONDS = 2L
private const val COMPLETION_TIMEOUT_SECONDS = 15L
private const val SHUTDOWN_TIMEOUT_SECONDS = 5L
private const val MAX_DIAGNOSTICS = 8
