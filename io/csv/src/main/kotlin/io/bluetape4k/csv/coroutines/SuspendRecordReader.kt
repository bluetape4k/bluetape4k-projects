package io.bluetape4k.csv.coroutines

import io.bluetape4k.csv.Record
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
interface SuspendRecordReader : Closeable {

    /**
     * 입력 스트림을 읽어 [Record]를 원하는 타입으로 변환한 [Flow]를 반환합니다.
     *
     * @param input 읽을 CSV/TSV 입력 스트림
     * @param encoding 텍스트 디코딩에 사용할 문자셋
     * @param skipHeaders `true`이면 첫 행을 헤더로 처리하여 결과에서 제외
     * @param transform 레코드를 결과 타입으로 변환하는 suspend 함수
     */
    fun <T> read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
        transform: suspend (Record) -> T,
    ): Flow<T>

    /**
     * 입력 스트림을 [Record] 그대로 방출하는 [Flow]로 읽습니다.
     */
    fun read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
    ): Flow<Record> = read(input, encoding, skipHeaders) { it }
}
