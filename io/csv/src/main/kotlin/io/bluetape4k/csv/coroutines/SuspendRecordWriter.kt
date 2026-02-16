package io.bluetape4k.csv.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import java.io.Closeable

/**
 * Coroutines 환경하에서 CSV/TSV Record를 쓰는 Writer 입니다.
 *
 * ```
 * val writer = SuspendCsvRecordWriter(output)
 * writer.writeHeaders(listOf("name", "age"))
 * writer.writeRow(listOf("Alice", 20))
 * writer.writeRow(listOf("Bob", 30))
 * writer.close()
 * ```
 */
interface SuspendRecordWriter: Closeable {

    /**
     * CSV/TSV 파일의 헤더 행을 비동기로 기록합니다.
     *
     * @param headers 헤더 이름들
     */
    suspend fun writeHeaders(headers: Iterable<String>)

    /**
     * CSV/TSV 파일의 헤더 행을 비동기로 기록합니다.
     *
     * @param headers 헤더 이름들
     */
    suspend fun writeHeaders(vararg headers: String) {
        writeHeaders(headers.toList())
    }

    /**
     * 하나의 데이터 행을 비동기로 기록합니다.
     *
     * @param row 기록할 데이터 행
     */
    suspend fun writeRow(row: Iterable<*>)

    /**
     * 엔티티를 변환 함수를 통해 데이터 행으로 변환하여 비동기로 기록합니다.
     *
     * @param entity 기록할 엔티티
     * @param mapper 엔티티를 데이터 행으로 변환하는 함수
     */
    suspend fun <T> writeRow(entity: T, mapper: suspend (T) -> Iterable<*>) {
        writeRow(mapper(entity))
    }

    /**
     * 여러 데이터 행을 비동기로 순차 기록합니다.
     *
     * @param rows 기록할 데이터 행들
     */
    suspend fun writeAll(rows: Sequence<Iterable<*>>)

    /**
     * 여러 엔티티를 변환 함수를 통해 데이터 행으로 변환하여 비동기로 순차 기록합니다.
     *
     * @param entities 기록할 엔티티들
     * @param transform 엔티티를 데이터 행으로 변환하는 함수
     */
    suspend fun <T> writeAll(entities: Sequence<T>, transform: suspend (T) -> Iterable<*>) {
        writeAll(entities.asFlow().map(transform))
    }

    /**
     * [Iterable]로 전달되는 여러 데이터 행을 비동기로 순차 기록합니다.
     *
     * @param rows 기록할 데이터 행들
     */
    suspend fun writeAll(rows: Iterable<Iterable<*>>) {
        writeAll(rows.asFlow())
    }

    /**
     * [Iterable]로 전달되는 여러 엔티티를 변환 함수를 통해 데이터 행으로 변환하여 비동기로 순차 기록합니다.
     *
     * @param entities 기록할 엔티티들
     * @param transform 엔티티를 데이터 행으로 변환하는 함수
     */
    suspend fun <T> writeAll(entities: Iterable<T>, transform: suspend (T) -> Iterable<*>) {
        writeAll(entities.asFlow().map(transform))
    }

    /**
     * [Flow]로 전달되는 데이터 행을 비동기로 수집하여 기록합니다.
     *
     * @param rows 기록할 데이터 행들의 Flow
     */
    suspend fun writeAll(rows: Flow<Iterable<*>>)

    /**
     * [Flow]로 전달되는 엔티티를 변환 함수를 통해 데이터 행으로 변환하여 비동기로 수집하여 기록합니다.
     *
     * @param entities 기록할 엔티티들의 Flow
     * @param transform 엔티티를 데이터 행으로 변환하는 함수
     */
    suspend fun <T> writeAll(entities: Flow<T>, transform: suspend (T) -> Iterable<*>) {
        writeAll(entities.map(transform))
    }
}
