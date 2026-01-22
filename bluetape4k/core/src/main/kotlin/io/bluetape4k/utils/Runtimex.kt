package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.Serializable
import java.net.URL
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * [Runtime]이 제공하는 다양한 정보를 조회할 수 있는 유틸리티 클래스
 */
object Runtimex: KLogging() {

    private val runtime by lazy { Runtime.getRuntime() }

    /**
     * 사용 가능한 프로세서 수
     */
    @JvmField
    val availableProcessors = runtime.availableProcessors()

    /**
     * 현재 JVM의 메모리 사용량 정보를 조회합니다.
     */
    val totalMemory: Long
        get() = runtime.totalMemory()

    /**
     * 현재 JVM의 최대 메모리 사용량 정보를 조회합니다.
     */
    val maxMemory: Long
        get() = runtime.maxMemory()

    /**
     * 사용 가능한 메모리 정보를 조회합니다.
     */
    val availableMemory: Long
        get() = freeMemory + (maxMemory - totalMemory)

    /**
     * 사용 가능한 메모리의 백분율 정보를 조회합니다.
     */
    val availableMemoryPercent: Double
        get() = availableMemory.toDouble() * 100.0 / runtime.maxMemory()

    /**
     * 사용 가능한 메모리의 백분율 정보를 조회합니다.
     */
    val freeMemory: Long
        get() = runtime.freeMemory()

    /**
     * 사용 가능한 메모리의 백분율 정보를 조회합니다.
     */
    val freeMemoryPercent: Double
        get() = freeMemory.toDouble() / runtime.totalMemory()

    /**
     * 사용 중인 메모리 정보를 조회합니다.
     */
    val usedMemory: Long
        get() = totalMemory - freeMemory

    private const val TWO_GIGA = 2_000_000_000

    /**
     * 메모리를 정리합니다.
     */
    fun compactMemory() {
        try {
            val unused = arrayListOf<ByteArray>()
            repeat(128) {
                unused.add(ByteArray(TWO_GIGA))
            }
        } catch (ignored: OutOfMemoryError) {
            // NOP
        }
        log.info { "Start Compact memory..." }
        System.gc()
    }

    /**
     * [clazz]의 위치를 조회합니다.
     */
    fun classLocation(clazz: Class<*>): URL =
        clazz.protectionDomain.codeSource.location

    /**
     * JVM 종료 시에 실행될 Cleanup code 를 추가합니다.
     *
     * ```
     * Runtimex.addShutdownHook {
     *    log.info { "Shutdown hook is called." }
     *    // Do something...
     *    log.info { "Shutdown hook is finished." }
     * }
     *
     * @param block VM 종료 시에 실행될 코드 블럭
     */
    inline fun addShutdownHook(crossinline block: () -> Unit) {
        Runtime.getRuntime().addShutdownHook(thread(start = false) { block() })
    }

    /** Process 실행 결과를 담은 Value Object */
    data class ProcessResult(val existCode: Int, val out: String): Serializable

    /**
     * Process 실행 시 결과를 [ProcessResult] 정보에 담아 반환합니다.
     */
    fun run(process: Process): ProcessResult = ByteArrayOutputStream().use { bos ->
        val outputCapture = StreamGobbler(process.inputStream, bos, "out>")
        val errorCapture = StreamGobbler(process.errorStream, bos, "out>")

        outputCapture.start()
        errorCapture.start()

        val result = process.waitFor()

        outputCapture.waitFor()
        errorCapture.waitFor()

        ProcessResult(result, bos.toString(Charsets.UTF_8.name()))
    }

    /**
     * Consumes a stream
     */
    internal class StreamGobbler(
        private val input: InputStream,
        private val output: OutputStream? = null,
        private val prefix: String? = null,
    ): Thread() {

        private val lock = ReentrantLock()
        private val condition = lock.newCondition()

        private var end = false

        private val prefixBytes: ByteArray get() = prefix?.let { prefix.toByteArray() } ?: ByteArray(0)
        private val newLineBytes: ByteArray get() = System.lineSeparator().toByteArray()

        override fun run() {
            InputStreamReader(input).use { isr ->
                BufferedReader(isr).use { br ->
                    output?.let {
                        var line: String? = br.readLine()
                        while (line != null) {
                            output.write(prefixBytes)
                            output.write(line.toByteArray())
                            output.write(newLineBytes)
                            line = br.readLine()
                        }
                        output.flush()
                    }
                }
            }

            lock.withLock {
                end = true
                condition.signalAll()
            }
        }

        /**
         * Lock 이 풀릴 때까지 기다린다.
         */
        fun waitFor() {
            try {
                lock.withLock {
                    if (!end) {
                        condition.await()
                    }
                }
            } catch (ignored: InterruptedException) {
                // Ignore exception.
            }
        }
    }
}
