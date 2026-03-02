package io.bluetape4k.csv

import com.univocity.parsers.common.record.Record
import com.univocity.parsers.tsv.TsvParser
import com.univocity.parsers.tsv.TsvParserSettings
import io.bluetape4k.logging.KLogging
import java.io.InputStream
import java.nio.charset.Charset

/**
 * univocity TSV 파서를 사용하는 [RecordReader] 구현체입니다.
 *
 * ## 동작/계약
 * - [settings]로 생성한 [TsvParser]가 입력을 순차 파싱합니다.
 * - [skipHeaders]가 `true`면 첫 레코드를 drop 합니다.
 * - 반환 시퀀스는 lazy로 동작합니다.
 *
 * ```kotlin
 * val names = TsvRecordReader()
 *     .read(input, skipHeaders = true) { it.getString("name") }
 *     .toList()
 * // names == listOf("Alice", "Bob")
 * ```
 */
class TsvRecordReader(
    private val settings: TsvParserSettings = DefaultTsvParserSettings,
): RecordReader {

    companion object: KLogging()

    /**
     * TSV 입력 스트림을 읽어 변환 결과 시퀀스를 반환합니다.
     *
     * ## 동작/계약
     * - `iterateRecords(input, encoding)` 결과를 [Sequence]로 노출합니다.
     * - [skipHeaders]가 `true`면 첫 행이 결과에서 제외됩니다.
     * - 파싱/변환 실패 예외는 전파됩니다.
     *
     * ```kotlin
     * val ids = TsvRecordReader().read(input, skipHeaders = true) { it.getLong("id") }.toList()
     * // ids == listOf(1L, 2L)
     * ```
     */
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
