package io.bluetape4k.csv

import io.bluetape4k.csv.internal.CsvLexer
import io.bluetape4k.logging.KLogging
import java.io.InputStream
import java.nio.charset.Charset

/**
 * 자체 [CsvLexer]를 사용하는 CSV [RecordReader] 구현체입니다.
 *
 * ## 동작/계약
 * - [settings]로 생성한 [CsvLexer]가 입력을 순차 파싱합니다.
 * - [skipHeaders]가 `true`면 첫 행을 헤더로 읽어 저장하고 이후 행부터 반환합니다.
 * - 반환 시퀀스는 lazy이며 소비 시점에 변환 람다가 실행됩니다.
 *
 * ```kotlin
 * val names = CsvRecordReader()
 *     .read(input, skipHeaders = true) { it.getString("name") }
 *     .toList()
 * // names == listOf("Alice", "Bob")
 * ```
 */
class CsvRecordReader(
    private val settings: CsvSettings = CsvSettings.DEFAULT,
) : RecordReader {

    companion object : KLogging()

    /**
     * CSV 입력 스트림을 읽어 변환 결과 시퀀스를 반환합니다.
     *
     * @param input 읽을 CSV 입력 스트림
     * @param encoding 텍스트 디코딩에 사용할 문자셋
     * @param skipHeaders `true`이면 첫 행을 헤더로 처리하여 반환 시퀀스에서 제외
     * @param transform 레코드를 결과 타입으로 변환하는 함수
     */
    override fun <T> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        transform: (Record) -> T,
    ): Sequence<T> = sequence {
        CsvLexer(input.reader(encoding), settings, skipHeaders).use { lexer ->
            while (lexer.hasNext()) {
                yield(transform(lexer.next()))
            }
        }
    }
}
