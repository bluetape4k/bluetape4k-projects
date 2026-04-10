package io.bluetape4k.batch.core.dsl

import io.bluetape4k.batch.api.BatchJobRepository
import io.bluetape4k.batch.core.BatchJob
import io.bluetape4k.batch.core.BatchStep
import io.bluetape4k.batch.core.InMemoryBatchJobRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank

/**
 * [BatchJob] DSL 빌더.
 *
 * [batchJob] 함수를 통해 진입하세요.
 *
 * ```kotlin
 * val job = batchJob("importUsers") {
 *     repository(myJdbcRepository)
 *     params("date" to "2026-04-10", "env" to "prod")
 *     step<UserCsv, UserEntity>("loadStep") {
 *         reader(csvReader)
 *         processor { csv -> UserEntity(csv.name, csv.email) }
 *         writer(jdbcWriter)
 *         chunkSize(500)
 *     }
 * }
 * ```
 *
 * @property name Job 이름 (blank 불가)
 */
@BatchDsl
class BatchJobBuilder(private val name: String) {

    companion object : KLogging()

    private var _repository: BatchJobRepository = InMemoryBatchJobRepository()
    private var _params: Map<String, Any> = emptyMap()
    private val _steps = mutableListOf<BatchStep<*, *>>()

    /**
     * Job 실행 이력을 저장할 [BatchJobRepository]를 설정합니다.
     * 기본값: [InMemoryBatchJobRepository]
     *
     * @param repo 사용할 [BatchJobRepository] 구현체
     */
    fun repository(repo: BatchJobRepository) {
        _repository = repo
    }

    /**
     * Job 실행 파라미터를 key-value 쌍으로 설정합니다.
     *
     * @param pairs 파라미터 쌍 (e.g., `"date" to "2026-04-10"`)
     */
    fun params(vararg pairs: Pair<String, Any>) {
        _params = pairs.toMap()
    }

    /**
     * Job 실행 파라미터를 Map으로 설정합니다.
     *
     * @param map 파라미터 맵
     */
    fun params(map: Map<String, Any>) {
        _params = map
    }

    /**
     * 내부적으로 Step을 등록합니다. [step] 인라인 함수에서 호출합니다.
     *
     * @param step 등록할 [BatchStep]
     */
    @PublishedApi
    internal fun addStep(step: BatchStep<*, *>) {
        _steps.add(step)
    }

    /**
     * DSL로 [BatchStep]을 구성하고 Job에 추가합니다.
     *
     * ```kotlin
     * step<Order, OrderEntity>("importStep") {
     *     reader(orderReader)
     *     processor { order -> OrderEntity.from(order) }
     *     writer(entityWriter)
     *     chunkSize(200)
     *     skipPolicy(SkipPolicy { e, count -> e is IllegalArgumentException && count < 50 })
     * }
     * ```
     *
     * @param I Reader 출력 타입
     * @param O Writer 입력 타입
     * @param stepName Step 이름 (blank 불가)
     * @param block [BatchStepBuilder] DSL 블록
     */
    inline fun <reified I : Any, reified O : Any> step(
        stepName: String,
        block: BatchStepBuilder<I, O>.() -> Unit,
    ) {
        stepName.requireNotBlank("stepName")
        addStep(BatchStepBuilder<I, O>(stepName).apply(block).build())
    }

    /**
     * 설정된 값으로 [BatchJob]을 빌드합니다.
     *
     * @return 구성된 [BatchJob]
     * @throws IllegalArgumentException name이 blank이거나 step이 하나도 없는 경우
     */
    fun build(): BatchJob {
        name.requireNotBlank("name")
        require(_steps.isNotEmpty()) { "최소 1개의 step이 필요합니다 — job='$name'" }
        return BatchJob(
            name = name,
            params = _params,
            steps = _steps.toList(),
            repository = _repository,
        )
    }
}
