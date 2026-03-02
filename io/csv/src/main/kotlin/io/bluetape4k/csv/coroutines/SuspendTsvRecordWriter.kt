package io.bluetape4k.csv.coroutines

import com.univocity.parsers.tsv.TsvWriter
import com.univocity.parsers.tsv.TsvWriterSettings
import io.bluetape4k.csv.DefaultTsvWriterSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.Flow
import java.io.Writer

/**
 * TSV 행 데이터를 코루틴 방식으로 기록하는 [SuspendRecordWriter] 구현체입니다.
 *
 * ## 동작/계약
 * - 헤더/행 입력을 리스트로 복사해 [TsvWriter]에 전달합니다.
 * - [Flow] 입력은 collect 순서대로 기록됩니다.
 * - [close]는 내부 writer 종료 예외를 무시합니다.
 *
 * ```kotlin
 * SuspendTsvRecordWriter(output).use { writer ->
 *     writer.writeHeaders("name", "age")
 *     writer.writeRow(listOf("Alice", 20))
 * }
 * // output 첫 데이터 행 == "Alice\t20"
 * ```
 */
class SuspendTsvRecordWriter private constructor(
    private val writer: TsvWriter,
): SuspendRecordWriter {

    companion object: KLoggingChannel() {
        /**
         * 기존 [TsvWriter]를 감싸는 [SuspendTsvRecordWriter]를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 writer 인스턴스를 그대로 사용합니다.
         *
         * ```kotlin
         * val writer = SuspendTsvRecordWriter(TsvWriter(output))
         * // writer != null
         * ```
         */
        @JvmStatic
        operator fun invoke(writer: TsvWriter): SuspendTsvRecordWriter {
            return SuspendTsvRecordWriter(writer)
        }

        /**
         * [Writer]와 설정으로 [SuspendTsvRecordWriter]를 생성합니다.
         *
         * ## 동작/계약
         * - [settings] 기반 새 [TsvWriter]를 만들고 [invoke]에 위임합니다.
         *
         * ```kotlin
         * val writer = SuspendTsvRecordWriter(output, DefaultTsvWriterSettings)
         * // writer != null
         * ```
         */
        @JvmStatic
        operator fun invoke(
            writer: Writer,
            settings: TsvWriterSettings = DefaultTsvWriterSettings,
        ): SuspendTsvRecordWriter {
            return invoke(TsvWriter(writer, settings))
        }
    }

    /**
     * 헤더 행을 기록합니다.
     *
     * ## 동작/계약
     * - [headers]를 리스트로 복사해 기록합니다.
     *
     * ```kotlin
     * writer.writeHeaders(listOf("id", "name"))
     * // 첫 행 == "id\tname"
     * ```
     */
    override suspend fun writeHeaders(headers: Iterable<String>) {
        writer.writeHeaders(headers.toList())
    }

    /**
     * 데이터 행 1건을 기록합니다.
     *
     * ## 동작/계약
     * - [row]를 리스트로 복사해 기록합니다.
     *
     * ```kotlin
     * writer.writeRow(listOf("Alice", 20))
     * // 다음 행 == "Alice\t20"
     * ```
     */
    override suspend fun writeRow(row: Iterable<*>) {
        writer.writeRow(row.toList())
    }

    /**
     * 여러 데이터 행을 순차 기록합니다.
     *
     * ## 동작/계약
     * - [rows]를 앞에서부터 순차 소비합니다.
     *
     * ```kotlin
     * writer.writeAll(sequenceOf(listOf("A", 1), listOf("B", 2)))
     * // 데이터 행 2건 기록됨
     * ```
     */
    override suspend fun writeAll(rows: Sequence<Iterable<*>>) {
        rows.forEach { writeRow(it) }
    }

    /**
     * [Flow] 데이터 행을 수집해 순차 기록합니다.
     *
     * ## 동작/계약
     * - collect 순서대로 행을 기록합니다.
     *
     * ```kotlin
     * writer.writeAll(kotlinx.coroutines.flow.flowOf(listOf("A", 1)))
     * // 데이터 행 1건 기록됨
     * ```
     */
    override suspend fun writeAll(rows: Flow<Iterable<*>>) {
        rows.collect { writeRow(it) }
    }

    /**
     * 내부 [TsvWriter]를 닫습니다.
     *
     * ## 동작/계약
     * - 종료 중 예외는 무시됩니다.
     *
     * ```kotlin
     * val writer = SuspendTsvRecordWriter(output)
     * writer.close()
     * // close 호출 후 예외 없음
     * ```
     */
    override fun close() {
        runCatching { writer.close() }
    }
}
