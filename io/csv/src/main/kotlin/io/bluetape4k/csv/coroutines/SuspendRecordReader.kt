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
 * val reader = CoCsvRecordReader()
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
     * CSV 나 TSV 등의 파일을 읽어드립니다.
     *
     * @param input InputStream 읽어드릴 Stream
     * @param encoding Charset 인코등 정보
     * @param skipHeaders Boolean 파일에 헤더가 있다면 skip 할지 여부
     * @return Flow<Record> 읽어드린 Record를 제공하는 [Flow]
     */
    fun <T: Any> read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
        transform: (Record) -> T,
    ): Flow<T>

    /**
     * CSV 나 TSV 등의 파일을 읽어드립니다.
     *
     * @param input InputStream 읽어드릴 Stream
     * @param encoding Charset 인코등 정보
     * @param skipHeaders Boolean 파일에 헤더가 있다면 skip 할지 여부
     * @return Flow<Record> 읽어드린 Record를 제공하는 [Flow]
     */
    fun read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
    ): Flow<Record> {
        return read(input, encoding, skipHeaders) { it }
    }
}
