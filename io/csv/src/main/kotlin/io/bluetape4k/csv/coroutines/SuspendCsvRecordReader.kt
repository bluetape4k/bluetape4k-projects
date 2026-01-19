package io.bluetape4k.csv.coroutines

import com.univocity.parsers.common.record.Record
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import io.bluetape4k.csv.DefaultCsvParserSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
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
 * @property settings CSV 파서 설정
 */
class SuspendCsvRecordReader(
    private val settings: CsvParserSettings = DefaultCsvParserSettings,
): SuspendRecordReader {

    companion object: KLoggingChannel()

    override fun <T: Any> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        transform: (Record) -> T,
    ): Flow<T> {
        return CsvParser(settings)
            .iterateRecords(input, encoding)
            .asFlow()
            .drop(if (skipHeaders) 1 else 0)
            .map { transform(it) }
    }

    override fun close() {
        // Nothing to do.
    }
}
