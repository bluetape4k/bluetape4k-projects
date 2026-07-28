package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import org.apache.fory.Fory
import org.apache.fory.ThreadSafeFory
import org.apache.fory.config.CompatibleMode
import org.apache.fory.config.Language
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

private const val FORY_OUTPUT_LIMIT_MESSAGE = "Serialized output exceeds Int.MAX_VALUE bytes."

private class ForyBorrowedTargetWriteFailure(
    cause: Throwable,
): RuntimeException(cause)

private class ForyOutputCountOverflowFailure(
    cause: ArithmeticException,
): IllegalStateException(FORY_OUTPUT_LIMIT_MESSAGE, cause)

private class ForyCallerOwnedCountingOutputStream(
    private val target: OutputStream,
): OutputStream() {

    var written: Int = 0
        private set

    override fun write(value: Int) {
        val next = checkedCount(1)
        writeToTarget { target.write(value) }
        written = next
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        val next = checkedCount(length)
        writeToTarget { target.write(bytes, offset, length) }
        written = next
    }

    override fun flush() = Unit

    override fun close() = Unit

    private fun checkedCount(length: Int): Int =
        try {
            Math.addExact(written, length)
        } catch (failure: ArithmeticException) {
            throw ForyOutputCountOverflowFailure(failure)
        }

    private inline fun writeToTarget(block: () -> Unit) {
        try {
            block()
        } catch (failure: Throwable) {
            throw ForyBorrowedTargetWriteFailure(failure)
        }
    }
}

/**
 * A [BinarySerializer] backed by [Fory](https://fory.apache.org/).
 *
 * > **Security warning:** The default configuration uses `requireClassRegistration(false)` and is intended only
 * > for trusted payloads. Use [secureFory] and an explicit registration allow-list when deserializing across a
 * > boundary that is not fully controlled by the application.
 *
 * ```
 * val serializer = ForyBinarySerializer()
 * val bytes = serializer.serialize("Hello, World!")
 * val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
 * ```
 *
 * [deserializeFrom] delegates a duplicate of the caller's bounded range to Fory's `ByteBuffer` API.
 * [serializeBinaryToStream] writes through Fory's `OutputStream` overload and avoids only the returned/handoff
 * payload array. The target is borrowed synchronously and is not retained, flushed, or closed. Fory still uses its
 * reusable internal `MemoryBuffer`, so this is not a zero-copy serializer.
 * [serializeTo] intentionally keeps the BinarySerializer ByteArray compatibility fallback because
 * Fory's MemoryBuffer output may grow by replacing caller-provided storage.
 * The [issue #1039 evidence](https://github.com/bluetape4k/bluetape4k-projects/blob/develop/docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md)
 * found input allocation inconclusive; output remains an ergonomic-only fallback.
 */
class ForyBinarySerializer(
    private val fory: ThreadSafeFory = DefaultFory,
): AbstractBinarySerializer() {

    companion object: KLogging() {
        @JvmStatic
        private val DefaultFory: ThreadSafeFory by lazy {
            Fory.builder()
                .withLanguage(Language.JAVA)
                .withCompatibleMode(CompatibleMode.COMPATIBLE)
                .withAsyncCompilation(true)
                .withRefTracking(true)
                .withRefCopy(true)
                .withCodegen(true)
                .withStringCompressed(true)
                .requireClassRegistration(false)
                .buildThreadSafeForyPool(threadSafeForyPoolSize())
        }

        // SCHEMA_CONSISTENT: 필드명 대신 위치 기반 ID 직렬화 — COMPATIBLE 대비 페이로드 크기 및 CPU 절감
        // asyncCompilation=false: 동기 JIT 컴파일로 warmup 내 완전 최적화 보장
        // refTracking=false: 순환참조 없는 DTO 그래프에서 레퍼런스 테이블 오버헤드 제거
        @JvmStatic
        private val FastFory: ThreadSafeFory by lazy {
            Fory.builder()
                .withLanguage(Language.JAVA)
                .withCompatibleMode(CompatibleMode.SCHEMA_CONSISTENT)
                .withAsyncCompilation(false)
                .withRefTracking(false)
                .withRefCopy(false)
                .withCodegen(true)
                .withStringCompressed(false)
                .requireClassRegistration(false)
                .buildThreadSafeForyPool(threadSafeForyPoolSize())
        }

        /**
         * `SCHEMA_CONSISTENT`와 비활성화된 reference tracking으로 설정된 [ForyBinarySerializer]를 반환합니다.
         *
         * Throughput depends on the payload and runtime profile. Use committed benchmark evidence for the exact
         * workload instead of assuming a fixed uplift over the default serializer.
         *
         * ## Suitable use cases
         * - **Ephemeral** caches such as Redis or message queues, where data lifetime follows deployments.
         * - **Fixed-schema** DTOs whose class structure does not change.
         * - **DAG-shaped** object graphs without cyclic references.
         *
         * ## Do not use when
         * - Binary data is **persisted** in a database or file. `SCHEMA_CONSISTENT` is not wire-compatible with
         *   the default `COMPATIBLE` format and cannot read those existing payloads.
         * - Object graphs contain **cyclic references**. With `refTracking=false`, they can loop or overflow the stack.
         * - Schema evolution requires fields to be **added or removed** at runtime.
         *
         * ```kotlin
         * // Correct: ephemeral cache and fixed-schema DTO
         * val serializer = ForyBinarySerializer.fast()
         * val bytes = serializer.serialize(myDto)
         *
         * // Incorrect: trying to decode default Fory bytes with fast()
         * val legacy = ForyBinarySerializer()   // COMPATIBLE mode
         * val legacyBytes = legacy.serialize(myDto)
         * serializer.deserialize<MyDto>(legacyBytes) // failure or invalid result
         * ```
         */
        @JvmStatic
        fun fast(): ForyBinarySerializer = ForyBinarySerializer(FastFory)

        /**
         * 클래스 등록이 강제되는 보안 [ThreadSafeFory]를 생성합니다.
         *
         * ## 동작/계약
         * - `requireClassRegistration(true)` 설정으로 등록된 클래스만 직렬화/역직렬화를 허용합니다.
         * - 미등록 클래스 직렬화 시도 시 즉시 예외가 발생하므로, 의도치 않은 타입 노출을 방지합니다.
         * - [classes]에 포함한 클래스와 그 필드 타입이 모두 등록되어 있어야 합니다.
         *
         * ```kotlin
         * // 보안 Fory 생성 후 ForyBinarySerializer에 주입
         * val secureFory = ForyBinarySerializer.secureFory(
         *     MyData::class.java,
         *     MyOtherData::class.java,
         * )
         * val serializer = ForyBinarySerializer(fory = secureFory)
         *
         * // 등록된 클래스는 직렬화 가능
         * val bytes = serializer.serialize(MyData("hello"))
         *
         * // 미등록 클래스는 직렬화 시 BinarySerializationException 발생
         * serializer.serialize(UnregisteredClass())  // throws!
         * ```
         *
         * @param classes 직렬화를 허용할 사용자 정의 클래스 목록
         */
        @JvmStatic
        fun secureFory(vararg classes: Class<*>): ThreadSafeFory =
            Fory.builder()
                .withLanguage(Language.JAVA)
                .withCompatibleMode(CompatibleMode.COMPATIBLE)
                .withAsyncCompilation(true)
                .withRefTracking(true)
                .withRefCopy(true)
                .withCodegen(true)
                .withStringCompressed(true)
                .requireClassRegistration(true)   // 보안: 등록된 클래스만 허용
                .buildThreadSafeForyPool(threadSafeForyPoolSize()).also { fory ->
                    classes.forEach { fory.register(it) }
                }

        private fun threadSafeForyPoolSize(): Int =
            maxOf(2, 2 * Runtime.getRuntime().availableProcessors())
    }

    /**
     * I/O 직렬화에서 `doSerialize` 함수를 제공합니다.
     */
    override fun doSerialize(graph: Any): ByteArray {
        return fory.serialize(graph)
    }

    @Throws(IOException::class)
    override fun serializeBinaryToStream(graph: Any?, target: OutputStream): Int {
        val source = graph ?: return 0
        val output = ForyCallerOwnedCountingOutputStream(target)

        return try {
            fory.serialize(output, source)
            output.written
        } catch (failure: ForyBorrowedTargetWriteFailure) {
            throw checkNotNull(failure.cause)
        } catch (failure: ForyOutputCountOverflowFailure) {
            throw failure
        } catch (failure: Throwable) {
            throw BinarySerializationException("Fail to serialize. graphType=${source.javaClass.name}", failure)
        }
    }

    /**
     * I/O 직렬화에서 `doDeserialize` 함수를 제공합니다.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        return fory.deserialize(bytes) as? T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> deserializeFrom(source: ByteBuffer): T? {
        val sourceSize = source.remaining()
        if (sourceSize == 0) return null

        return try {
            fory.deserialize(source.duplicate()) as? T
        } catch (failure: Throwable) {
            throwBufferDeserializationFailure(sourceSize, failure)
        }
    }
}
