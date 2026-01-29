package io.bluetape4k.csv.coroutines

import io.bluetape4k.collections.eclipse.toFastList
import kotlinx.coroutines.flow.Flow
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

    suspend fun writeHeaders(headers: Iterable<String>)

    suspend fun writeHeaders(vararg headers: String) {
        writeHeaders(headers.toFastList())
    }

    suspend fun writeRow(row: Iterable<*>)

    suspend fun <T> writeRow(entity: T, mapper: (T) -> Iterable<*>) {
        writeRow(mapper(entity))
    }

    suspend fun writeAll(rows: Sequence<Iterable<*>>)

    suspend fun <T> writeAll(entities: Sequence<T>, transform: (T) -> Iterable<*>) {
        writeAll(entities.map(transform))
    }

    suspend fun writeAll(rows: Flow<Iterable<*>>)

    suspend fun <T> writeAll(entities: Flow<T>, transform: (T) -> Iterable<*>) {
        writeAll(entities.map(transform))
    }
}
