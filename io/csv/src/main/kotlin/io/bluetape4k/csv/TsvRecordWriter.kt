package io.bluetape4k.csv

import com.univocity.parsers.tsv.TsvWriter
import com.univocity.parsers.tsv.TsvWriterSettings
import io.bluetape4k.logging.KLogging
import java.io.Writer

/**
 * univocity [TsvWriter]를 감싼 [RecordWriter] 구현체입니다.
 *
 * ## 동작/계약
 * - 헤더/행 입력을 리스트로 복사해 writer에 전달합니다.
 * - [writeAll]은 입력 시퀀스를 순서대로 소비합니다.
 * - [close]는 writer 종료 예외를 무시합니다.
 *
 * ```kotlin
 * TsvRecordWriter(output).use { writer ->
 *     writer.writeHeaders("name", "price")
 *     writer.writeRow(listOf("pen", 1000))
 * }
 * // output 첫 데이터 행 == "pen\t1000"
 * ```
 */
class TsvRecordWriter private constructor(
    private val writer: TsvWriter,
): RecordWriter {

    companion object: KLogging() {
        /**
         * 기존 [TsvWriter]를 감싸는 [TsvRecordWriter]를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 [tsvWriter] 인스턴스를 그대로 사용합니다.
         *
         * ```kotlin
         * val writer = TsvRecordWriter(TsvWriter(output))
         * // writer != null
         * ```
         */
        @JvmStatic
        operator fun invoke(tsvWriter: TsvWriter): TsvRecordWriter {
            return TsvRecordWriter(tsvWriter)
        }

        /**
         * [Writer]와 설정으로 [TsvRecordWriter]를 생성합니다.
         *
         * ## 동작/계약
         * - [settings] 기반 새 [TsvWriter]를 만들어 [invoke]에 위임합니다.
         *
         * ```kotlin
         * val writer = TsvRecordWriter(output, DefaultTsvWriterSettings)
         * // writer != null
         * ```
         */
        @JvmStatic
        operator fun invoke(
            writer: Writer,
            settings: TsvWriterSettings = DefaultTsvWriterSettings,
        ): TsvRecordWriter {
            return invoke(TsvWriter(writer, settings))
        }
    }

    /**
     * 헤더 행을 기록합니다.
     *
     * ## 동작/계약
     * - [headers]를 리스트로 복사해 한 행으로 기록합니다.
     *
     * ```kotlin
     * writer.writeHeaders(listOf("id", "name"))
     * // 첫 행 == "id\tname"
     * ```
     */
    override fun writeHeaders(headers: Iterable<String>) {
        writer.writeHeaders(headers.toList())
    }

    /**
     * 데이터 행 1건을 기록합니다.
     *
     * ## 동작/계약
     * - [rows]를 리스트로 복사해 기록합니다.
     *
     * ```kotlin
     * writer.writeRow(listOf("Alice", 20))
     * // 다음 행 == "Alice\t20"
     * ```
     */
    override fun writeRow(rows: Iterable<*>) {
        writer.writeRow(rows.toList())
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
     * 내부 [TsvWriter]를 닫습니다.
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
        runCatching { writer.close() }
    }
}
