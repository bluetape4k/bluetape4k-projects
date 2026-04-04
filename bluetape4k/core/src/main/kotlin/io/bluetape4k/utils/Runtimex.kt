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
object Runtimex : KLogging() {
    private val runtime by lazy { Runtime.getRuntime() }

    /**
     * 사용 가능한 프로세서 수
     *
     * ```kotlin
     * val cpus = Runtimex.availableProcessors  // 예: 8
     * ```
     */
    @JvmField
    val availableProcessors = runtime.availableProcessors()

    /**
     * 현재 JVM에 할당된 총 메모리 크기(바이트)를 조회합니다.
     *
     * ```kotlin
     * val total = Runtimex.totalMemory  // 예: 268435456 (256 MB)
     * ```
     */
    val totalMemory: Long
        get() = runtime.totalMemory()

    /**
     * 현재 JVM이 사용할 수 있는 최대 메모리 크기(바이트)를 조회합니다.
     *
     * ```kotlin
     * val max = Runtimex.maxMemory  // -Xmx 설정값
     * ```
     */
    val maxMemory: Long
        get() = runtime.maxMemory()

    /**
     * JVM이 추가 할당 가능한 메모리(바이트)를 조회합니다.
     *
     * ```kotlin
     * val avail = Runtimex.availableMemory  // freeMemory + (maxMemory - totalMemory)
     * ```
     */
    val availableMemory: Long
        get() = freeMemory + (maxMemory - totalMemory)

    /**
     * 사용 가능한 메모리의 백분율을 조회합니다.
     *
     * ```kotlin
     * val pct = Runtimex.availableMemoryPercent  // 예: 75.0 (75%)
     * ```
     */
    val availableMemoryPercent: Double
        get() = availableMemory.toDouble() * 100.0 / runtime.maxMemory()

    /**
     * JVM GC 후 즉시 사용 가능한 여유 메모리(바이트)를 조회합니다.
     *
     * ```kotlin
     * val free = Runtimex.freeMemory  // 현재 힙 여유 메모리
     * ```
     */
    val freeMemory: Long
        get() = runtime.freeMemory()

    /**
     * 여유 메모리 비율(0.0 ~ 1.0)을 조회합니다.
     *
     * ```kotlin
     * val ratio = Runtimex.freeMemoryPercent  // 예: 0.4 (40%)
     * ```
     */
    val freeMemoryPercent: Double
        get() = freeMemory.toDouble() / runtime.totalMemory()

    /**
     * 현재 사용 중인 메모리(바이트)를 조회합니다.
     *
     * ```kotlin
     * val used = Runtimex.usedMemory  // totalMemory - freeMemory
     * ```
     */
    val usedMemory: Long
        get() = totalMemory - freeMemory

    private const val TWO_GIGA = 2_000_000_000

    /**
     * 가비지 컬렉션을 유도하여 메모리를 정리합니다.
     *
     * ```kotlin
     * Runtimex.compactMemory()  // System.gc() 호출로 GC 유도
     * ```
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
     * [clazz]가 로드된 JAR 또는 디렉토리의 URL을 조회합니다.
     *
     * ```kotlin
     * val url = Runtimex.classLocation(String::class.java)
     * // 예: file:/usr/lib/jvm/java-21/lib/rt.jar
     * ```
     *
     * @param clazz 위치를 조회할 클래스
     * @return 클래스 소스의 [URL]
     */
    fun classLocation(clazz: Class<*>): URL = clazz.protectionDomain.codeSource.location

    /**
     * JVM 종료 시에 실행될 Cleanup code 를 추가합니다.
     *
     * ```kotlin
     * Runtimex.addShutdownHook {
     *     println("JVM 종료 중 — 자원 정리")
     *     // 예: 커넥션 풀 해제, 임시 파일 삭제 등
     * }
     * ```
     *
     * @param block VM 종료 시에 실행될 코드 블럭
     */
    inline fun addShutdownHook(crossinline block: () -> Unit) {
        Runtime.getRuntime().addShutdownHook(thread(start = false) { block() })
    }

    /** Process 실행 결과를 담은 Value Object */
    data class ProcessResult(
        val exitCode: Int,
        val out: String,
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /**
     * 이미 시작된 [Process]의 표준 출력과 에러를 캡처하고 종료를 기다려 결과를 반환합니다.
     *
     * ```kotlin
     * val pb = ProcessBuilder("echo", "hello")
     * val result = Runtimex.run(pb.start())
     * // result.exitCode == 0
     * // result.out 에 "out>hello\n" 포함
     * ```
     *
     * @param process 실행 중인 프로세스
     * @return [ProcessResult] (종료 코드 + 출력 문자열)
     */
    fun run(process: Process): ProcessResult =
        ByteArrayOutputStream().use { bos ->
            val outputCapture = StreamGobbler(process.inputStream, bos, "out>")
            val errorCapture = StreamGobbler(process.errorStream, bos, "err>")

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
    ) : Thread() {
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
                    if (!end) condition.await()
                }
            } catch (ignored: InterruptedException) {
                // Ignore exception.
            }
        }
    }
}
