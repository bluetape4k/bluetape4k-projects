package io.bluetape4k.csv.coroutines

import com.univocity.parsers.common.record.Record
import kotlinx.coroutines.flow.Flow
import java.io.Closeable
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Coroutines 환경하에서 CSV/TSV Record를 읽는 Reader입니다.
 *
 * ```
 * val reader = SuspendCsvRecordReader()
 * val items:Flow<Item> = reader.read(input, Charsets.UTF_8, skipHeaders = true) { record ->
 *      // record 처리
 *      // record to item by recordMapper
 *      val name = record.getString("name")
 *      val age = record.getInt("age")
 *      // ...
 *      Item(name, age)
 * }
 * ```
 *
 */
interface SuspendRecordReader: Closeable {

    /**
     * CSV 나 TSV 등의 파일을 읽어들입니다.
     *
     * @param input 읽어들일 입력 스트림
     * @param encoding 인코딩 정보
     * @param skipHeaders 파일에 헤더가 있다면 skip 할지 여부
     * @param transform Record 를 원하는 타입으로 변환하는 함수
     * @return 읽어들인 Record를 제공하는 [Flow]
     */
    fun <T: Any> read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
        transform: (Record) -> T,
    ): Flow<T>

    /**
     * CSV 나 TSV 등의 파일을 읽어들입니다.
     *
     * @param input 읽어들일 입력 스트림
     * @param encoding 인코딩 정보
     * @param skipHeaders 파일에 헤더가 있다면 skip 할지 여부
     * @return 읽어들인 Record를 제공하는 [Flow]
     */
    fun read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
    ): Flow<Record> {
        return read(input, encoding, skipHeaders) { it }
    }
}
