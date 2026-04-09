package io.bluetape4k.spring.batch.exposed.integration

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
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
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.PlatformTransactionManager
import kotlin.system.measureTimeMillis

/**
 * 파티션 수 / VirtualThread 동시성 수별 처리 시간 벤치마크.
 *
 * CI 제외: `@Tag("benchmark")` — `./gradlew test -PexcludeTags="benchmark"`
 *
 * 로컬에서만 실행:
 * ```bash
 * ./gradlew :bluetape4k-spring-boot3-batch-exposed:test --tests "*ParallelQueryBenchmarkTest*" -PincludeTags="benchmark"
 * ```
 */
@Tag("benchmark")
class ParallelQueryBenchmarkTest : AbstractExposedBatchJobTest() {

    companion object : KLogging()

    @TestConfiguration
    class JobConfig(
        private val jobRepository: JobRepository,
        private val transactionManager: PlatformTransactionManager,
        private val database: Database,
    ) {
        @Bean(name = ["benchmarkMigrationJob"])
        fun migrationJob(): Job = partitionedBatchJob("benchmark-migration-job", jobRepository) {
            start(partitionedStep())
        }

        @Bean(name = ["benchmarkPartitionedStep"])
        fun partitionedStep(): Step = StepBuilder("benchmark-migration-manager", jobRepository)
            .partitioner("benchmark-migration-worker", rangePartitioner())
            .partitionHandler(partitionHandler())
            .build()

        @Bean(name = ["benchmarkRangePartitioner"])
        fun rangePartitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 8,
            database = database,
        )

        @Bean(name = ["benchmarkPartitionHandler"])
        fun partitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 8))
            gridSize = 8
        }

        @Bean(name = ["benchmarkWorkerStep"])
        fun workerStep(): Step = StepBuilder("benchmark-migration-worker", jobRepository)
            .chunk<SourceRecord, TargetRecord>(500, transactionManager)
            .reader(keysetReader())
            .processor(ItemProcessor { source ->
                TargetRecord(sourceName = source.name.uppercase(), transformedValue = source.value * 2)
            })
            .writer(itemWriter())
            .build()

        @Bean(name = ["benchmarkKeysetReader"])
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

        @Bean(name = ["benchmarkItemWriter"])
        fun itemWriter(): ExposedItemWriter<TargetRecord> = ExposedItemWriter(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        @Bean(name = ["benchmarkJobLauncherTestUtils"])
        fun jobLauncherTestUtils(
            @Qualifier("benchmarkMigrationJob") job: Job,
            jobLauncher: JobLauncher,
        ): JobLauncherTestUtils = JobLauncherTestUtils().apply {
            this.job = job
            this.jobRepository = this@JobConfig.jobRepository
            this.jobLauncher = jobLauncher
        }
    }

    @Autowired
    @Qualifier("benchmarkJobLauncherTestUtils")
    private lateinit var jobLauncherTestUtils: JobLauncherTestUtils

    @Test
    fun `8파티션 VirtualThread 처리 시간 측정`() {
        insertTestData(50_000)

        val elapsed = measureTimeMillis {
            val params = JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters()
            val execution = jobLauncherTestUtils.launchJob(params)
            execution.status shouldBeEqualTo BatchStatus.COMPLETED
        }
        log.info { "8파티션 VirtualThread 처리 시간: ${elapsed}ms (50,000건)" }
    }
}
