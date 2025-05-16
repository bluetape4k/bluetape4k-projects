package io.bluetape4k.csv.coroutines

import com.univocity.parsers.common.record.Record
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import io.bluetape4k.csv.DefaultCsvParserSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
 * @property settings CSV 파서 설정
 */
class CoCsvRecordReader(
    private val settings: CsvParserSettings = DefaultCsvParserSettings,
): CoRecordReader {

    companion object: KLoggingChannel()

    override fun <T: Any> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        recordMapper: (Record) -> T,
    ): Flow<T> = flow {
        val parser = CsvParser(settings)
        parser.iterateRecords(input, encoding)
            .drop(if (skipHeaders) 1 else 0)
            .forEach { record ->
                emit(recordMapper(record))
            }
    }

    override fun close() {
        // Nothing to do.
    }
}
