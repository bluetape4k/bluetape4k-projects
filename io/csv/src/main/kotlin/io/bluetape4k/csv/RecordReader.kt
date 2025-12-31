package io.bluetape4k.csv

import com.univocity.parsers.common.record.Record
import java.io.Closeable
import java.io.InputStream
import java.nio.charset.Charset

/**
 * CSV 포맷 형태의 파일 정보를 [Record] 로 읽어드리는 Reader 입니다.
 *
 * ```
 * val reader = CsvRecordReader()
 * val items:Sequence<Item> = reader.read(input, Charsets.UTF_8, skipHeaders = true) { record ->
 *     // record 처리
 *     // record to item by recordMapper
 *     record.getString("name")
 *     record.getInt("age")
 *     // ...
 *     Item(record.getString("name"), record.getInt("age"))
 * }
 * ```
 */
interface RecordReader: Closeable {

    /**
     * CSV 파일을 읽어들여서 [Record] 로 변환합니다.
     *
     * @param input CSV 파일의 입력 스트림
     * @param encoding CSV 파일의 인코딩
     * @param skipHeaders CSV 파일의 헤더를 건너뛸지 여부
     * @param transform Record 를 원하는 타입으로 변환하는 함수
     */
    fun <T> read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
        transform: (Record) -> T,
    ): Sequence<T>

    /**
     * CSV 파일을 읽어들여서 [Record] 로 변환합니다.
     *
     * @param input CSV 파일의 입력 스트림
     * @param encoding CSV 파일의 인코딩
     * @param skipHeaders CSV 파일의 헤더를 건너뛸지 여부
     * @return CSV 파일의 레코드들
     */
    fun read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
    ): Sequence<Record> {
        return read(input, encoding, skipHeaders) { it }
    }

    override fun close() {
        // NOOP
    }
}
