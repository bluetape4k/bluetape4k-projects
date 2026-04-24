package io.bluetape4k.csv

import io.bluetape4k.csv.internal.TsvLineWriter
import io.bluetape4k.logging.KLogging
import java.io.Writer

/**
 * [TsvLineWriter]를 감싼 [RecordWriter] 구현체입니다.
 *
 * ## 동작/계약
 * - 헤더/행 입력을 [TsvLineWriter]에 전달합니다.
 * - [writeAll]은 입력 시퀀스를 순서대로 소비합니다.
 * - [close]는 내부 writer를 닫습니다 (flush 포함).
 *
 * ```kotlin
 * TsvRecordWriter(output).use { writer ->
 *     writer.writeHeaders("name", "price")
 *     writer.writeRow(listOf("pen", 1000))
 * }
 * // output 첫 데이터 행 == "pen\t1000"
 * ```
 */
class TsvRecordWriter(
    writer: Writer,
    settings: TsvSettings = TsvSettings.DEFAULT,
) : RecordWriter {

    companion object : KLogging()

    private val lineWriter = TsvLineWriter(writer, settings)

    /**
     * 헤더 행을 기록합니다.
     *
     * ## 동작/계약
     * - [headers]를 TSV 형식으로 한 행 기록합니다.
     *
     * ```kotlin
     * writer.writeHeaders(listOf("id", "name"))
     * // 첫 행 == "id\tname"
     * ```
     */
    override fun writeHeaders(headers: Iterable<String>) {
        lineWriter.writeRow(headers)
    }

    /**
     * 데이터 행 1건을 기록합니다.
     *
     * ## 동작/계약
     * - [rows]를 TSV 형식으로 기록합니다.
     *
     * ```kotlin
     * writer.writeRow(listOf("Alice", 20))
     * // 다음 행 == "Alice\t20"
     * ```
     */
    override fun writeRow(rows: Iterable<*>) {
        lineWriter.writeRow(rows)
    }

    /**
     * 여러 데이터 행을 순차 기록합니다.
     *
     * ## 동작/계약
     * - [rows]를 순차 소비하며 각 행을 [writeRow]로 기록합니다.
     *
     * ```kotlin
     * writer.writeAll(sequenceOf(listOf("A", 1), listOf("B", 2)))
     * // 데이터 행 2건 기록됨
     * ```
     */
    override fun writeAll(rows: Sequence<Iterable<*>>) {
        rows.forEach { writeRow(it) }
    }

    /**
     * 내부 [TsvLineWriter]를 닫습니다 (flush 포함).
     *
     * ## 동작/계약
     * - 종료 중 예외는 무시됩니다.
     *
     * ```kotlin
     * val writer = TsvRecordWriter(output)
     * writer.close()
     * // close 호출 후 예외 없음
     * ```
     */
    override fun close() {
        runCatching { lineWriter.close() }
    }
}
