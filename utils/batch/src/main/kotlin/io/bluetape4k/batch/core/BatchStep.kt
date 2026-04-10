package io.bluetape4k.batch.core

import io.bluetape4k.batch.BatchDefaults
import io.bluetape4k.batch.api.BatchProcessor
import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.workflow.api.RetryPolicy
import kotlin.time.Duration

/**
 * 배치 처리 Step 정의. Reader → (Processor) → Writer 파이프라인을 구성한다.
 *
 * ## 파이프라인 흐름
 * ```
 * reader.open()
 *   → [reader.restoreFrom(checkpoint)]?
 *   → reader.read()* (chunkSize 단위)
 *   → processor.process(item)?
 *   → writer.write(chunk)
 *   → reader.onChunkCommitted()
 * reader.close() / writer.close()
 * ```
 *
 * ## 예시
 * ```kotlin
 * val step = BatchStep(
 *     name = "importUsers",
 *     chunkSize = 500,
 *     reader = myReader,
 *     processor = myProcessor,
 *     writer = myWriter,
 *     skipPolicy = SkipPolicy { cause, count -> cause is IllegalArgumentException && count < 100 },
 *     retryPolicy = RetryPolicy.DEFAULT,
 *     commitTimeout = 60.seconds,
 * )
 * ```
 *
 * @param I Reader 출력 타입
 * @param O Writer 입력 타입
 * @property name Step 이름. 빈 문자열 불허.
 * @property chunkSize 청크 단위 처리 크기. 양수여야 한다.
 * @property reader 데이터를 읽는 [BatchReader]
 * @property processor 아이템을 변환하는 [BatchProcessor]. null이면 변환 없이 통과 (I == O 일 때).
 * @property writer 데이터를 저장하는 [BatchWriter]
 * @property skipPolicy 처리 중 예외 발생 시 스킵 여부를 결정하는 [SkipPolicy]. 기본값: [SkipPolicy.NONE]
 * @property retryPolicy 청크 쓰기 실패 시 재시도 정책. 기본값: [RetryPolicy.NONE]
 * @property commitTimeout 청크 커밋 타임아웃. 0 이하면 타임아웃 미적용.
 */
class BatchStep<I : Any, O : Any>(
    val name: String,
    val chunkSize: Int = BatchDefaults.CHUNK_SIZE,
    val reader: BatchReader<I>,
    val processor: BatchProcessor<I, O>? = null,
    val writer: BatchWriter<O>,
    val skipPolicy: SkipPolicy = SkipPolicy.NONE,
    val retryPolicy: RetryPolicy = RetryPolicy.NONE,
    val commitTimeout: Duration = BatchDefaults.COMMIT_TIMEOUT,
) {
    init {
        name.requireNotBlank("name")
        chunkSize.requirePositiveNumber("chunkSize")
    }

    companion object : KLoggingChannel()
}
