package io.bluetape4k.csv.coroutines

import io.bluetape4k.csv.Record
import io.bluetape4k.csv.TsvSettings
import io.bluetape4k.csv.internal.TsvLexer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStream
import java.nio.charset.Charset

/**
 * 자체 [TsvLexer]를 사용하는 TSV [SuspendRecordReader] 구현체입니다.
 *
 * ## 동작/계약
 * - [TsvLexer] 결과를 [channelFlow]로 변환해 방출합니다.
 * - 레코드 처리 전마다 [ensureActive]로 취소를 협력적으로 확인합니다.
 * - 블로킹 IO는 [Dispatchers.IO]에서 실행됩니다.
 * - [skipHeaders]가 `true`면 첫 레코드를 헤더로 처리합니다.
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
    private val settings: TsvSettings = TsvSettings.DEFAULT,
) : SuspendRecordReader {

    companion object : KLoggingChannel()

    /**
     * TSV 입력 스트림을 읽어 변환된 [Flow]를 반환합니다.
     *
     * [channelFlow]를 사용해 취소 협력(cooperative cancellation)을 보장하며,
     * [Dispatchers.IO]에서 블로킹 IO를 수행합니다.
     *
     * @param input 읽을 TSV 입력 스트림
     * @param encoding 텍스트 디코딩에 사용할 문자셋
     * @param skipHeaders `true`이면 첫 행을 헤더로 처리
     * @param transform 레코드를 결과 타입으로 변환하는 suspend 함수
     */
    override fun <T> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        transform: suspend (Record) -> T,
    ): Flow<T> = channelFlow {
        TsvLexer(input.reader(encoding), settings, skipHeaders).use { lexer ->
            while (lexer.hasNext()) {
                ensureActive()
                send(transform(lexer.next()))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun close() {
        // Nothing to do
    }
}
