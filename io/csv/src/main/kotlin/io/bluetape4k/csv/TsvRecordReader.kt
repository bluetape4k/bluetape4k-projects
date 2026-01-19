package io.bluetape4k.csv

import com.univocity.parsers.common.record.Record
import com.univocity.parsers.tsv.TsvParser
import com.univocity.parsers.tsv.TsvParserSettings
import io.bluetape4k.logging.KLogging
import java.io.InputStream
import java.nio.charset.Charset

/**
 * TSV 포맷의 파일을 읽어드리는 [RecordReader] 입니다.
 *
 * ```
 * val reader = TsvRecordReader()
 * val items:Sequence<Item> = reader.read(input, Charsets.UTF_8, skipHeaders = true) { record ->
 *     // record 처리
 *     // record to item by recordMapper
 *     record.getString("name")
 *     record.getInt("age")
 *     // ...
 *     Item(record.getString("name"), record.getInt("age"))
 * }
 * ```
 *
 * @property settings TSV 파서 설정
 */
class TsvRecordReader(
    private val settings: TsvParserSettings = DefaultTsvParserSettings,
): RecordReader {

    companion object: KLogging()

    override fun <T> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        transform: (Record) -> T,
    ): Sequence<T> {
        return TsvParser(settings).iterateRecords(input, encoding)
            .asSequence()
            .drop(if (skipHeaders) 1 else 0)
            .map { record -> transform(record) }
    }
}
