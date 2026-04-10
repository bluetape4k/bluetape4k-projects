package io.bluetape4k.batch.core.dsl

import io.bluetape4k.batch.BatchDefaults
import io.bluetape4k.batch.api.BatchProcessor
import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.batch.core.BatchStep
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.workflow.api.RetryPolicy
import kotlin.time.Duration

/**
 * [BatchStep] DSL 빌더.
 *
 * [BatchJobBuilder.step] 블록 내에서 사용합니다.
 *
 * ```kotlin
 * step<UserCsv, UserEntity>("importStep") {
 *     reader(csvReader)
 *     processor { csv -> UserEntity(csv.name, csv.email) }
 *     writer(entityWriter)
 *     chunkSize(500)
 *     skipPolicy(SkipPolicy { e, count -> e is DataFormatException && count < 100 })
 *     retryPolicy(RetryPolicy.DEFAULT)
 *     commitTimeout(60.seconds)
 * }
 * ```
 *
 * @param I Reader 출력 타입
 * @param O Writer 입력 타입
 * @property name Step 이름 (blank 불가)
 */
@BatchDsl
class BatchStepBuilder<I : Any, O : Any>(val name: String) {

    companion object : KLogging()

    private var _reader: BatchReader<I>? = null
    private var _processor: BatchProcessor<I, O>? = null
    private var _writer: BatchWriter<O>? = null
    private var _chunkSize: Int = BatchDefaults.CHUNK_SIZE
    private var _skipPolicy: SkipPolicy = SkipPolicy.NONE
    private var _retryPolicy: RetryPolicy = RetryPolicy.NONE
    private var _commitTimeout: Duration = BatchDefaults.COMMIT_TIMEOUT

    /**
     * 데이터를 읽는 [BatchReader]를 설정합니다.
     *
     * @param reader 사용할 [BatchReader] 구현체
     */
    fun reader(reader: BatchReader<I>) {
        _reader = reader
    }

    /**
     * 데이터를 저장하는 [BatchWriter]를 설정합니다.
     *
     * @param writer 사용할 [BatchWriter] 구현체
     */
    fun writer(writer: BatchWriter<O>) {
        _writer = writer
    }

    /**
     * 아이템을 변환하는 [BatchProcessor]를 설정합니다.
     *
     * @param processor 사용할 [BatchProcessor] 구현체
     */
    fun processor(processor: BatchProcessor<I, O>) {
        _processor = processor
    }

    /**
     * suspend 람다로 [BatchProcessor]를 설정합니다.
     *
     * ```kotlin
     * processor { item -> transform(item) }
     * ```
     *
     * @param block 아이템 변환 람다 (null 반환 시 해당 아이템 필터링)
     */
    fun processor(block: suspend (I) -> O?) {
        _processor = BatchProcessor { block(it) }
    }

    /**
     * 청크 단위 처리 크기를 설정합니다.
     *
     * @param size 청크 크기 (양수여야 함)
     */
    fun chunkSize(size: Int) {
        size.requirePositiveNumber("chunkSize")
        _chunkSize = size
    }

    /**
     * 아이템 처리 중 예외 발생 시 skip 여부를 결정하는 [SkipPolicy]를 설정합니다.
     *
     * @param policy 사용할 [SkipPolicy]
     */
    fun skipPolicy(policy: SkipPolicy) {
        _skipPolicy = policy
    }

    /**
     * chunk 쓰기 실패 시 재시도 정책을 설정합니다.
     *
     * @param policy 사용할 [RetryPolicy]
     */
    fun retryPolicy(policy: RetryPolicy) {
        _retryPolicy = policy
    }

    /**
     * 청크 커밋 타임아웃을 설정합니다. 0 이하면 타임아웃 미적용.
     *
     * @param timeout 커밋 타임아웃 [Duration]
     */
    fun commitTimeout(timeout: Duration) {
        _commitTimeout = timeout
    }

    /**
     * 설정된 값으로 [BatchStep]을 빌드합니다.
     *
     * @return 구성된 [BatchStep]
     * @throws IllegalArgumentException name이 blank이거나 reader/writer가 설정되지 않은 경우
     */
    fun build(): BatchStep<I, O> {
        name.requireNotBlank("name")
        requireNotNull(_reader) { "reader must be set for step '$name'" }
        requireNotNull(_writer) { "writer must be set for step '$name'" }
        return BatchStep(
            name = name,
            chunkSize = _chunkSize,
            reader = _reader!!,
            processor = _processor,
            writer = _writer!!,
            skipPolicy = _skipPolicy,
            retryPolicy = _retryPolicy,
            commitTimeout = _commitTimeout,
        )
    }
}
