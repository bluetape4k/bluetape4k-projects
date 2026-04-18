package io.bluetape4k.csv.coroutines

import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.csv.internal.CsvLineWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Writer

/**
 * CSV 행 데이터를 코루틴 방식으로 기록하는 [SuspendRecordWriter] 구현체입니다.
 *
 * ## 동작/계약
 * - 헤더/행 입력을 [CsvLineWriter]에 전달합니다.
 * - [Mutex]로 동시 쓰기를 보호합니다.
 * - [Flow] 입력은 collect 순서대로 기록됩니다.
 * - [close]는 내부 writer를 닫습니다 (flush 포함).
 *
 * ```kotlin
 * SuspendCsvRecordWriter(output).use { writer ->
 *     writer.writeHeaders("name", "age")
 *     writer.writeRow(listOf("Alice", 20))
 * }
 * // output 첫 데이터 행 == "Alice,20"
 * ```
 */
class SuspendCsvRecordWriter(
    writer: Writer,
    settings: CsvSettings = CsvSettings.DEFAULT,
) : SuspendRecordWriter {

    companion object : KLoggingChannel()

    private val lineWriter = CsvLineWriter(writer, settings)
    private val mutex = Mutex()

    /**
     * 헤더 행을 기록합니다.
     *
     * ## 동작/계약
     * - [Mutex]로 동시 접근을 직렬화합니다.
     * - [headers]를 CSV 형식으로 한 행 기록합니다.
     *
     * ```kotlin
     * writer.writeHeaders(listOf("id", "name"))
     * // 첫 행 == "id,name"
     * ```
     */
    override suspend fun writeHeaders(headers: Iterable<String>) {
        mutex.withLock { lineWriter.writeRow(headers) }
    }

    /**
     * 데이터 행 1건을 기록합니다.
     *
     * ## 동작/계약
     * - [Mutex]로 동시 접근을 직렬화합니다.
     * - [row]를 CSV 형식으로 기록합니다.
     *
     * ```kotlin
     * writer.writeRow(listOf("Alice", 20))
     * // 다음 행 == "Alice,20"
     * ```
     */
    override suspend fun writeRow(row: Iterable<*>) {
        mutex.withLock { lineWriter.writeRow(row) }
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
     * 내부 [CsvLineWriter]를 닫습니다 (flush 포함).
     *
     * ## 동작/계약
     * - 종료 중 예외는 무시됩니다.
     *
     * ```kotlin
     * val writer = SuspendCsvRecordWriter(output)
     * writer.close()
     * // close 호출 후 예외 없음
     * ```
     */
    override fun close() {
        runCatching { lineWriter.close() }
    }
}
