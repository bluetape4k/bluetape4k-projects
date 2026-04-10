package io.bluetape4k.batch.core.dsl

import io.bluetape4k.batch.core.BatchJob

/**
 * [BatchJob] DSL 마커 어노테이션.
 *
 * [BatchDsl]이 붙은 빌더 클래스 내에서는 상위 스코프의 빌더를 암묵적으로 접근할 수 없습니다.
 */
@DslMarker
annotation class BatchDsl

/**
 * [BatchJob] DSL 진입점.
 *
 * ```kotlin
 * val job = batchJob("myJob") {
 *     repository(MyRepository())
 *     params("env" to "prod")
 *     step<Long, MyEntity>("loadStep") {
 *         reader(myReader)
 *         writer(myWriter)
 *         chunkSize(500)
 *     }
 * }
 * val report = job.run()
 * ```
 *
 * @param name Job 이름
 * @param block [BatchJobBuilder] DSL 블록
 * @return 구성된 [BatchJob] 인스턴스
 */
inline fun batchJob(
    name: String,
    block: BatchJobBuilder.() -> Unit,
): BatchJob = BatchJobBuilder(name).apply(block).build()
