package io.bluetape4k.spring.batch.exposed.dsl

import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.builder.SimpleJobBuilder
import org.springframework.batch.core.partition.PartitionHandler
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder

/**
 * Partitioned Batch Job을 간결하게 생성하는 DSL 확장.
 *
 * 사용 예시:
 * ```kotlin
 * val job = partitionedBatchJob("parallel-migrate", jobRepository) {
 *     start(partitionedStep)
 * }
 * ```
 *
 * @param name Job 이름
 * @param jobRepository [JobRepository]
 * @param block [JobBuilder] 설정 람다
 */
fun partitionedBatchJob(
    name: String,
    jobRepository: JobRepository,
    block: JobBuilder.() -> SimpleJobBuilder,
): Job = JobBuilder(name, jobRepository).block().build()

/**
 * Exposed Partitioned Step을 생성하는 DSL 확장.
 *
 * 사용 예시:
 * ```kotlin
 * val partitionedStep = stepBuilder("partitionedStep")
 *     .exposedPartitionedStep(partitioner, partitionHandler)
 * ```
 *
 * @param partitioner [ExposedRangePartitioner] 인스턴스
 * @param handler [PartitionHandler] 인스턴스
 */
fun StepBuilder.exposedPartitionedStep(
    partitioner: ExposedRangePartitioner,
    handler: PartitionHandler,
): Step = this.partitioner("worker", partitioner)
    .partitionHandler(handler)
    .build()
