package io.bluetape4k.csv.coroutines

import com.univocity.parsers.csv.CsvWriter
import com.univocity.parsers.csv.CsvWriterSettings
import io.bluetape4k.csv.DefaultCsvWriterSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.Flow
import java.io.Writer

/**
 * CSV 포맷으로 데이터를 파일로 쓰는 [SuspendRecordWriter] 입니다.
 *
 * ```
 * val writer = SuspendCsvRecordWriter(output)
 * writer.writeHeaders(listOf("name", "age"))
 * writer.writeRow(listOf("Alice", 20))
 * writer.writeRow(listOf("Bob", 30))
 * writer.close()
 * ```
 *
 * @property writer CSV writer
 */
class SuspendCsvRecordWriter private constructor(
    private val writer: CsvWriter,
): SuspendRecordWriter {

    companion object: KLoggingChannel() {
        /**
         * [CsvWriter]를 사용하여 [SuspendCsvRecordWriter] 인스턴스를 생성합니다.
         *
         * @param writer CSV writer
         * @return SuspendCsvRecordWriter 인스턴스
         */
        @JvmStatic
        operator fun invoke(writer: CsvWriter): SuspendCsvRecordWriter {
            return SuspendCsvRecordWriter(writer)
        }

        /**
         * [Writer]와 설정을 사용하여 [SuspendCsvRecordWriter] 인스턴스를 생성합니다.
         *
         * @param writer 출력 스트림
         * @param settings CSV writer 설정
         * @return SuspendCsvRecordWriter 인스턴스
         */
        @JvmStatic
        operator fun invoke(
            writer: Writer,
            settings: CsvWriterSettings = DefaultCsvWriterSettings,
        ): SuspendCsvRecordWriter {
            return invoke(CsvWriter(writer, settings))
        }
    }

    /**
     * CSV 파일의 헤더 행을 비동기로 기록합니다.
     *
     * @param headers 헤더 이름들
     */
    override suspend fun writeHeaders(headers: Iterable<String>) {
        writer.writeHeaders(headers.toList())
    }

    /**
     * 하나의 CSV 데이터 행을 비동기로 기록합니다.
     *
     * @param row 기록할 데이터 행
     */
    override suspend fun writeRow(row: Iterable<*>) {
        writer.writeRow(row.toList())
    }

    /**
     * 여러 CSV 데이터 행을 비동기로 순차 기록합니다.
     *
     * @param rows 기록할 데이터 행들
     */
    override suspend fun writeAll(rows: Sequence<Iterable<*>>) {
        rows.forEach { writeRow(it) }
    }

    /**
     * [Flow]로 전달되는 CSV 데이터 행을 비동기로 수집하여 기록합니다.
     *
     * @param rows 기록할 데이터 행들의 Flow
     */
    override suspend fun writeAll(rows: Flow<Iterable<*>>) {
        rows.collect { writeRow(it) }
    }

    /**
     * CSV/TSV 처리 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        runCatching { writer.close() }
    }
}
