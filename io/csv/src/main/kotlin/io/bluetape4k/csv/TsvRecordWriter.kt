package io.bluetape4k.csv

import com.univocity.parsers.tsv.TsvWriter
import com.univocity.parsers.tsv.TsvWriterSettings
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import java.io.Writer

/**
 * TSV 포맷으로 데이터를 파일로 쓰는 [RecordWriter] 입니다.
 *
 * ```
 * val writer = TsvRecordWriter(output)
 * writer.writeHeaders(listOf("name", "age"))
 * writer.writeRow(listOf("Alice", 20))
 * writer.writeRow(listOf("Bob", 30))
 * writer.close()
 * ```
 *
 * @property writer TSV writer
 */
class TsvRecordWriter private constructor(
    private val writer: TsvWriter,
): RecordWriter {

    companion object: KLogging() {
        /**
         * [TsvWriter]를 사용하여 [TsvRecordWriter] 인스턴스를 생성합니다.
         *
         * @param tsvWriter TSV writer
         * @return TsvRecordWriter 인스턴스
         */
        @JvmStatic
        operator fun invoke(tsvWriter: TsvWriter): TsvRecordWriter {
            return TsvRecordWriter(tsvWriter)
        }

        /**
         * [Writer]와 설정을 사용하여 [TsvRecordWriter] 인스턴스를 생성합니다.
         *
         * @param writer 출력 스트림
         * @param settings TSV writer 설정
         * @return TsvRecordWriter 인스턴스
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
     * TSV 파일의 헤더 행을 기록합니다.
     *
     * @param headers 헤더 이름들
     */
    override fun writeHeaders(headers: Iterable<String>) {
        writer.writeHeaders(headers.toFastList())
    }

    /**
     * 하나의 TSV 데이터 행을 기록합니다.
     *
     * @param rows 기록할 데이터 행
     */
    override fun writeRow(rows: Iterable<*>) {
        writer.writeRow(rows.toFastList())
    }

    /**
     * 여러 TSV 데이터 행을 순차적으로 기록합니다.
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
