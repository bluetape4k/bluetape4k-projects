package io.bluetape4k.csv

import java.io.Closeable
import java.io.InputStream
import java.nio.charset.Charset

/**
 * CSV/TSV 입력 스트림을 [Record] 시퀀스로 읽는 동기 Reader 계약입니다.
 *
 * ## 동작/계약
 * - [read]는 입력 스트림을 순차 소비하며, [skipHeaders]가 `true`면 첫 행을 건너뜁니다.
 * - 반환값은 lazy [Sequence]이며 소비 시점에 실제 파싱이 진행됩니다.
 * - 수신 객체 내부 상태를 변경하지 않고 변환 결과를 새 시퀀스로 제공합니다.
 *
 * ```kotlin
 * val names = CsvRecordReader()
 *     .read(input, skipHeaders = true) { it.getString("name") }
 *     .toList()
 * // names == listOf("Alice", "Bob")
 * ```
 */
interface RecordReader : Closeable {

    /**
     * 입력 스트림을 읽어 [Record]를 원하는 타입으로 변환합니다.
     *
     * @param input 읽을 CSV/TSV 입력 스트림입니다.
     * @param encoding 텍스트 디코딩에 사용할 문자셋입니다.
     * @param skipHeaders 첫 행(헤더) 건너뛰기 여부입니다.
     * @param transform 레코드를 결과 타입으로 변환하는 함수입니다.
     */
    fun <T> read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
        transform: (Record) -> T,
    ): Sequence<T>

    /**
     * 입력 스트림을 [Record] 시퀀스로 읽습니다.
     *
     * 내부적으로 `transform = { it }`를 사용해 [read]에 위임합니다.
     */
    fun read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
    ): Sequence<Record> = read(input, encoding, skipHeaders) { it }

    /**
     * Reader 리소스를 닫습니다. 기본 구현은 no-op.
     */
    override fun close() {
        // NOOP
    }
}
