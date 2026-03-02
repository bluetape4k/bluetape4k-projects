package io.bluetape4k.csv.coroutines

import com.univocity.parsers.common.record.Record
import kotlinx.coroutines.flow.Flow
import java.io.Closeable
import java.io.InputStream
import java.nio.charset.Charset

/**
 * CSV/TSV 입력을 코루틴 [Flow]로 읽는 Reader 계약입니다.
 *
 * ## 동작/계약
 * - [read]는 입력 스트림을 순차 파싱해 [Flow]로 방출합니다.
 * - [skipHeaders]가 `true`면 첫 행을 제외합니다.
 * - 반환 Flow는 cold stream이며 collect 시점에 파싱이 시작됩니다.
 *
 * ```kotlin
 * val names = SuspendCsvRecordReader()
 *     .read(input, skipHeaders = true) { it.getString("name") }
 *     .toList()
 * // names == listOf("Alice", "Bob")
 * ```
 */
interface SuspendRecordReader: Closeable {

    /**
     * 입력 스트림을 읽어 [Record]를 원하는 타입으로 변환한 [Flow]를 반환합니다.
     *
     * ## 동작/계약
     * - [transform]은 각 레코드마다 suspend로 실행됩니다.
     * - 파싱/변환 예외는 collect 호출자에게 전파됩니다.
     *
     * ```kotlin
     * val ids = SuspendCsvRecordReader()
     *     .read(input, skipHeaders = true) { it.getLong("id") }
     *     .toList()
     * // ids == listOf(1L, 2L)
     * ```
     */
    fun <T> read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
        transform: suspend (Record) -> T,
    ): Flow<T>

    /**
     * 입력 스트림을 [Record] 그대로 방출하는 [Flow]로 읽습니다.
     *
     * ## 동작/계약
     * - `transform = { it }` 경로로 [read]에 위임합니다.
     *
     * ```kotlin
     * val rows = SuspendCsvRecordReader().read(input, skipHeaders = true).toList()
     * // rows.size == 2
     * ```
     */
    fun read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
    ): Flow<Record> {
        return read(input, encoding, skipHeaders) { it }
    }
}
