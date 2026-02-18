package io.bluetape4k.csv

import com.univocity.parsers.csv.CsvWriter
import com.univocity.parsers.csv.CsvWriterSettings
import io.bluetape4k.logging.KLogging
import java.io.Writer

/**
 * CSV 포맷으로 데이터를 출력하는 Writer 입니다.
 *
 * ```
 * val writer = CsvRecordWriter(output)
 * writer.writeHeaders(listOf("name", "age"))
 * writer.writeRow(listOf("Alice", 20))
 * writer.writeRow(listOf("Bob", 30))
 * writer.close()
 * ```
 *
 * @property writer CSV writer
 */
class CsvRecordWriter private constructor(
    private val writer: CsvWriter,
): RecordWriter {

    companion object: KLogging() {
        /**
         * [CsvWriter]를 사용하여 [CsvRecordWriter] 인스턴스를 생성합니다.
         *
         * @param csvWriter CSV writer
         * @return CsvRecordWriter 인스턴스
         */
        @JvmStatic
        operator fun invoke(csvWriter: CsvWriter): CsvRecordWriter {
            return CsvRecordWriter(csvWriter)
        }

        /**
         * [Writer]와 설정을 사용하여 [CsvRecordWriter] 인스턴스를 생성합니다.
         *
         * @param writer 출력 스트림
         * @param settings CSV writer 설정
         * @return CsvRecordWriter 인스턴스
         */
        @JvmStatic
        operator fun invoke(
            writer: Writer,
            settings: CsvWriterSettings = DefaultCsvWriterSettings,
        ): CsvRecordWriter {
            return invoke(CsvWriter(writer, settings))
        }
    }

    /**
     * CSV 파일의 헤더 행을 기록합니다.
     *
     * @param headers 헤더 이름들
     */
    override fun writeHeaders(headers: Iterable<String>) {
        writer.writeHeaders(headers.toList())
    }

    /**
     * 하나의 CSV 데이터 행을 기록합니다.
     *
     * @param rows 기록할 데이터 행
     */
    override fun writeRow(rows: Iterable<*>) {
        writer.writeRow(rows.toList())
    }

    /**
     * 여러 CSV 데이터 행을 순차적으로 기록합니다.
     *
     * @param rows 기록할 데이터 행들
     */
    override fun writeAll(rows: Sequence<Iterable<*>>) {
        rows.forEach { writeRow(it) }
    }

    /**
     * CSV/TSV 처리 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        runCatching { writer.close() }
    }
}
