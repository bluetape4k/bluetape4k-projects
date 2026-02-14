package io.bluetape4k.csv.coroutines

import com.univocity.parsers.common.record.Record
import com.univocity.parsers.tsv.TsvParser
import com.univocity.parsers.tsv.TsvParserSettings
import io.bluetape4k.csv.DefaultTsvParserSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Coroutines 환경 하에서 TSV 포맷의 Record 를 읽어드립니다.
 *
 * ```
 * val reader = SuspendTsvRecordReader()
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
class SuspendTsvRecordReader(
    private val settings: TsvParserSettings = DefaultTsvParserSettings,
): SuspendRecordReader {

    companion object: KLoggingChannel()

    /**
     * CSV/TSV 처리에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override fun <T: Any> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        transform: (Record) -> T,
    ): Flow<T> {
        return TsvParser(settings)
            .iterateRecords(input, encoding)
            .asFlow()
            .drop(if (skipHeaders) 1 else 0)
            .map { transform(it) }
    }

    /**
     * CSV/TSV 처리 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        // Nothing to do
    }
}
