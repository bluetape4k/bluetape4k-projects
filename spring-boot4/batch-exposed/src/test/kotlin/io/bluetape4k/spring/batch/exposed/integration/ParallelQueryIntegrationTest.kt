package io.bluetape4k.spring.batch.exposed.integration

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchJobTest
import io.bluetape4k.spring.batch.exposed.SourceRecord
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.TargetRecord
import io.bluetape4k.spring.batch.exposed.TargetTable
import io.bluetape4k.spring.batch.exposed.dsl.partitionedBatchJob
import io.bluetape4k.spring.batch.exposed.insertTestData
import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import io.bluetape4k.spring.batch.exposed.reader.ExposedKeysetItemReader
import io.bluetape4k.spring.batch.exposed.support.virtualThreadPartitionTaskExecutor
import io.bluetape4k.spring.batch.exposed.writer.ExposedItemWriter
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.test.JobOperatorTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.PlatformTransactionManager

/**
 * Partitioned Batch Job E2E 통합 테스트.
 *
 * - 10,000건 Source -> Target, 4파티션 VirtualThread 병렬 마이그레이션
 * - Spring Batch Job 상태 == [BatchStatus.COMPLETED]
 * - Target 행 수 == Source 행 수 (중복 없음)
 */
class ParallelQueryIntegrationTest : AbstractExposedBatchJobTest() {

    companion object : KLogging()

    @TestConfiguration
    class JobConfig(
        private val jobRepository: JobRepository,
        private val transactionManager: PlatformTransactionManager,
        private val database: Database,
    ) {
        @Bean(name = ["parallelMigrationJob"])
        fun migrationJob(): Job = partitionedBatchJob("parallel-migration-job", jobRepository) {
            start(partitionedStep())
        }

        @Bean(name = ["parallelPartitionedStep"])
        fun partitionedStep(): Step = StepBuilder("parallel-migration-manager", jobRepository)
            .partitioner("parallel-migration-worker", rangePartitioner())
            .partitionHandler(partitionHandler())
            .build()

        @Bean(name = ["parallelRangePartitioner"])
        fun rangePartitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 4,
            database = database,
        )

        @Bean(name = ["parallelPartitionHandler"])
        fun partitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 4))
            gridSize = 4
        }

        @Bean(name = ["parallelWorkerStep"])
        fun workerStep(): Step = StepBuilder("parallel-migration-worker", jobRepository)
            .chunk<SourceRecord, TargetRecord>(500)
            .transactionManager(transactionManager)
            .reader(keysetReader())
            .processor(ItemProcessor { source ->
                TargetRecord(
                    sourceName = source.name.uppercase(),
                    transformedValue = source.value * 2,
                )
            })
            .writer(itemWriter())
            .build()

        @Bean(name = ["parallelKeysetReader"])
        @org.springframework.batch.core.configuration.annotation.StepScope
        fun keysetReader(): ExposedKeysetItemReader<SourceRecord> = ExposedKeysetItemReader.forEntityId(
            table = SourceTable,
            pageSize = 500,
            rowMapper = { row ->
                SourceRecord(
                    id = row[SourceTable.id].value,
                    name = row[SourceTable.name],
                    value = row[SourceTable.value],
                )
            },
            database = database,
        )

        @Bean(name = ["parallelItemWriter"])
        fun itemWriter(): ExposedItemWriter<TargetRecord> = ExposedItemWriter(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        @Bean(name = ["parallelJobOperatorTestUtils"])
        fun jobOperatorTestUtils(
            @Qualifier("parallelMigrationJob") job: Job,
            jobOperator: JobOperator,
        ): JobOperatorTestUtils = JobOperatorTestUtils(jobOperator, jobRepository).apply {
            this.job = job
        }
    }

    @Autowired
    @Qualifier("parallelJobOperatorTestUtils")
    private lateinit var jobOperatorTestUtils: JobOperatorTestUtils

    @Test
    fun `10000건 Source에서 Target으로 4파티션 VirtualThread 병렬 마이그레이션`() {
        insertTestData(10_000)

        val jobParameters = JobParametersBuilder()
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters()

        val execution = jobOperatorTestUtils.startJob(jobParameters)

        execution.status shouldBeEqualTo BatchStatus.COMPLETED

        val targetCount = transaction(database) { TargetTable.selectAll().count() }
        targetCount shouldBeEqualTo 10_000L
    }
}
