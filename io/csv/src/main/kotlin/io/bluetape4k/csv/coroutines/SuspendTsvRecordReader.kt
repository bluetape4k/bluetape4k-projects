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
 * TSV 입력을 코루틴 [Flow]로 제공하는 [SuspendRecordReader] 구현체입니다.
 *
 * ## 동작/계약
 * - [TsvParser] 결과를 flow로 변환해 순차 방출합니다.
 * - [skipHeaders]가 `true`면 첫 레코드를 제외합니다.
 * - 파싱/변환 예외는 collect 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val names = SuspendTsvRecordReader()
 *     .read(input, skipHeaders = true) { it.getString("name") }
 *     .toList()
 * // names == listOf("Alice", "Bob")
 * ```
 */
class SuspendTsvRecordReader(
    private val settings: TsvParserSettings = DefaultTsvParserSettings,
): SuspendRecordReader {

    companion object: KLoggingChannel()

    /**
     * TSV 입력 스트림을 읽어 변환된 [Flow]를 반환합니다.
     *
     * ## 동작/계약
     * - `iterateRecords(input, encoding)` 결과를 flow로 노출합니다.
     * - [transform]은 각 레코드마다 suspend로 실행됩니다.
     *
     * ```kotlin
     * val ids = SuspendTsvRecordReader().read(input, skipHeaders = true) { it.getLong("id") }.toList()
     * // ids == listOf(1L, 2L)
     * ```
     */
    override fun <T> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        transform: suspend (Record) -> T,
    ): Flow<T> =
        TsvParser(settings)
            .iterateRecords(input, encoding)
            .asFlow()
            .drop(if (skipHeaders) 1 else 0)
            .map { transform(it) }

    /**
     * 리소스를 닫습니다.
     *
     * ## 동작/계약
     * - 현재 구현은 별도 리소스를 보유하지 않아 no-op입니다.
     *
     * ```kotlin
     * val reader = SuspendTsvRecordReader()
     * reader.close()
     * // close 호출 후 부작용 없음
     * ```
     */
    override fun close() {
        // Nothing to do
    }
}
