package io.bluetape4k.csv.coroutines

import com.univocity.parsers.tsv.TsvWriter
import com.univocity.parsers.tsv.TsvWriterSettings
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.csv.DefaultTsvWriterSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.Flow
import java.io.Writer

/**
 * TSV 포맷으로 데이터를 파일로 쓰는 [SuspendRecordWriter] 입니다.
 *
 * ```
 * val writer = SuspendTsvRecordWriter(output)
 * writer.writeHeaders(listOf("name", "age"))
 * writer.writeRow(listOf("Alice", 20))
 * writer.writeRow(listOf("Bob", 30))
 * writer.close()
 * ```
 *
 * @property writer TSV writer
 */
class SuspendTsvRecordWriter private constructor(
    private val writer: TsvWriter,
): SuspendRecordWriter {

    companion object: KLoggingChannel() {
        /**
         * [TsvWriter]를 사용하여 [SuspendTsvRecordWriter] 인스턴스를 생성합니다.
         *
         * @param writer TSV writer
         * @return SuspendTsvRecordWriter 인스턴스
         */
        @JvmStatic
        operator fun invoke(writer: TsvWriter): SuspendTsvRecordWriter {
            return SuspendTsvRecordWriter(writer)
        }

        /**
         * [Writer]와 설정을 사용하여 [SuspendTsvRecordWriter] 인스턴스를 생성합니다.
         *
         * @param writer 출력 스트림
         * @param settings TSV writer 설정
         * @return SuspendTsvRecordWriter 인스턴스
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
     * TSV 파일의 헤더 행을 비동기로 기록합니다.
     *
     * @param headers 헤더 이름들
     */
    override suspend fun writeHeaders(headers: Iterable<String>) {
        writer.writeHeaders(headers.toFastList())
    }

    /**
     * 하나의 TSV 데이터 행을 비동기로 기록합니다.
     *
     * @param row 기록할 데이터 행
     */
    override suspend fun writeRow(row: Iterable<*>) {
        writer.writeRow(row.toFastList())
    }

    /**
     * 여러 TSV 데이터 행을 비동기로 순차 기록합니다.
     *
     * @param rows 기록할 데이터 행들
     */
    override suspend fun writeAll(rows: Sequence<Iterable<*>>) {
        rows.forEach { writeRow(it) }
    }

    /**
     * [Flow]로 전달되는 TSV 데이터 행을 비동기로 수집하여 기록합니다.
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
