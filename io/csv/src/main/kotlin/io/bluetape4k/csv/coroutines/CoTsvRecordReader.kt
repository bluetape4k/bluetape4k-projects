package io.bluetape4k.csv.coroutines

import com.univocity.parsers.common.record.Record
import com.univocity.parsers.tsv.TsvParser
import com.univocity.parsers.tsv.TsvParserSettings
import io.bluetape4k.csv.DefaultTsvParserSettings
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Coroutines 환경 하에서 TSV 포맷의 Record 를 읽어드립니다.
 *
 * ```
 * val reader = CoTsvRecordReader()
 * val items:Flow<Item> = reader.read(input, Charsets.UTF_8, skipHeaders = true) { record ->
 *     // record 처리
 *     // record to item by recordMapper
 *     val name = record.getString("name")
 *     val age = record.getInt("age")
 *     // ...
 *     Item(name, age)
 * }
 * ```
 *
 * @property settings TSV 파서 설정
 */
class CoTsvRecordReader(
    private val settings: TsvParserSettings = DefaultTsvParserSettings,
): CoRecordReader {

    companion object: KLogging()

    override fun <T: Any> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        recordMapper: (Record) -> T,
    ): Flow<T> = flow {
        val parser = TsvParser(settings)

        parser.iterateRecords(input, encoding)
            .drop(if (skipHeaders) 1 else 0)
            .forEach { record ->
                emit(recordMapper(record))
            }
    }

    override fun close() {
        // Nothing to do
    }
}