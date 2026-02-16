package io.bluetape4k.csv

import io.bluetape4k.collections.eclipse.toFastList
import java.io.Closeable

/**
 * CSV/TSV 포맷의 데이터를 기록하는 Writer 인터페이스입니다.
 *
 * ```
 * val writer = CsvRecordWriter(output)
 * writer.writeHeaders(listOf("name", "age"))
 * writer.writeRow(listOf("Alice", 20))
 * writer.writeRow(listOf("Bob", 30))
 * writer.close()
 * ```
 */
interface RecordWriter: Closeable {

    /**
     * CSV/TSV 파일의 헤더 행을 기록합니다.
     *
     * @param headers 헤더 이름들
     */
    fun writeHeaders(headers: Iterable<String>)

    /**
     * CSV/TSV 파일의 헤더 행을 기록합니다.
     *
     * @param headers 헤더 이름들
     */
    fun writeHeaders(vararg headers: String) {
        writeHeaders(headers.toFastList())
    }

    /**
     * 하나의 데이터 행을 기록합니다.
     *
     * @param rows 기록할 데이터 행
     */
    fun writeRow(rows: Iterable<*>)

    /**
     * 엔티티를 변환 함수를 통해 데이터 행으로 변환하여 기록합니다.
     *
     * @param entity 기록할 엔티티
     * @param transform 엔티티를 데이터 행으로 변환하는 함수
     */
    fun <T> writeRow(entity: T, transform: (T) -> Iterable<*>) {
        writeRow(transform(entity))
    }

    /**
     * 여러 데이터 행을 순차적으로 기록합니다.
     *
     * @param rows 기록할 데이터 행들
     */
    fun writeAll(rows: Sequence<Iterable<*>>)

    /**
     * 여러 엔티티를 변환 함수를 통해 데이터 행으로 변환하여 순차적으로 기록합니다.
     *
     * @param entities 기록할 엔티티들
     * @param transform 엔티티를 데이터 행으로 변환하는 함수
     */
    fun <T> writeAll(entities: Sequence<T>, transform: (T) -> Iterable<*>) {
        writeAll(entities.map(transform))
    }
}
