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
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.configuration.annotation.StepScope
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
import kotlin.system.measureTimeMillis

/**
 * 순차(1파티션) vs 병렬(4/8파티션) 처리 시간 비교 벤치마크.
 *
 * - Worker Step/Reader/Writer는 공유, Partitioner/PartitionHandler만 파티션 수별로 분리
 * - 50,000건 기준 각 구성의 처리 시간을 로그로 출력
 *
 * CI 제외: `@Tag("benchmark")` — `./gradlew test -PexcludeTags="benchmark"`
 *
 * 로컬 실행:
 * ```bash
 * ./gradlew :bluetape4k-spring-boot4-batch-exposed:test \
 *   --tests "*PartitionComparisonBenchmarkTest*" -PincludeTags="benchmark"
 * ```
 */
@Tag("benchmark")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PartitionComparisonBenchmarkTest : AbstractExposedBatchJobTest() {

    companion object : KLogging()

    @TestConfiguration
    class JobConfig(
        private val jobRepository: JobRepository,
        private val transactionManager: PlatformTransactionManager,
        private val database: Database,
    ) {
        // ── 공유 Worker 컴포넌트 ─────────────────────────────────────────────

        @Bean(name = ["cmpWorkerStep"])
        fun workerStep(): Step = StepBuilder("cmp-migration-worker", jobRepository)
            .chunk<SourceRecord, TargetRecord>(500)
            .transactionManager(transactionManager)
            .reader(keysetReader())
            .processor(ItemProcessor { source ->
                TargetRecord(sourceName = source.name.uppercase(), transformedValue = source.value * 2)
            })
            .writer(itemWriter())
            .build()

        @Bean(name = ["cmpKeysetReader"])
        @StepScope
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

        @Bean(name = ["cmpItemWriter"])
        fun itemWriter(): ExposedItemWriter<TargetRecord> = ExposedItemWriter(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        // ── 1파티션 (순차) ───────────────────────────────────────────────────

        @Bean(name = ["cmpSeqJob"])
        fun seqJob(): Job = partitionedBatchJob("cmp-seq-job", jobRepository) {
            start(seqPartitionedStep())
        }

        @Bean(name = ["cmpSeqPartitionedStep"])
        fun seqPartitionedStep(): Step = StepBuilder("cmp-seq-manager", jobRepository)
            .partitioner("cmp-migration-worker", seqPartitioner())
            .partitionHandler(seqPartitionHandler())
            .build()

        @Bean(name = ["cmpSeqPartitioner"])
        fun seqPartitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 1,
            database = database,
        )

        @Bean(name = ["cmpSeqPartitionHandler"])
        fun seqPartitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 1))
            gridSize = 1
        }

        @Bean(name = ["cmpSeqJobOperatorTestUtils"])
        fun seqJobOperatorTestUtils(
            @Qualifier("cmpSeqJob") job: Job,
            jobOperator: JobOperator,
        ): JobOperatorTestUtils = JobOperatorTestUtils(jobOperator, jobRepository).apply {
            this.job = job
        }

        // ── 4파티션 (병렬) ───────────────────────────────────────────────────

        @Bean(name = ["cmpPar4Job"])
        fun par4Job(): Job = partitionedBatchJob("cmp-par4-job", jobRepository) {
            start(par4PartitionedStep())
        }

        @Bean(name = ["cmpPar4PartitionedStep"])
        fun par4PartitionedStep(): Step = StepBuilder("cmp-par4-manager", jobRepository)
            .partitioner("cmp-migration-worker", par4Partitioner())
            .partitionHandler(par4PartitionHandler())
            .build()

        @Bean(name = ["cmpPar4Partitioner"])
        fun par4Partitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 4,
            database = database,
        )

        @Bean(name = ["cmpPar4PartitionHandler"])
        fun par4PartitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 4))
            gridSize = 4
        }

        @Bean(name = ["cmpPar4JobOperatorTestUtils"])
        fun par4JobOperatorTestUtils(
            @Qualifier("cmpPar4Job") job: Job,
            jobOperator: JobOperator,
        ): JobOperatorTestUtils = JobOperatorTestUtils(jobOperator, jobRepository).apply {
            this.job = job
        }

        // ── 8파티션 (병렬) ───────────────────────────────────────────────────

        @Bean(name = ["cmpPar8Job"])
        fun par8Job(): Job = partitionedBatchJob("cmp-par8-job", jobRepository) {
            start(par8PartitionedStep())
        }

        @Bean(name = ["cmpPar8PartitionedStep"])
        fun par8PartitionedStep(): Step = StepBuilder("cmp-par8-manager", jobRepository)
            .partitioner("cmp-migration-worker", par8Partitioner())
            .partitionHandler(par8PartitionHandler())
            .build()

        @Bean(name = ["cmpPar8Partitioner"])
        fun par8Partitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 8,
            database = database,
        )

        @Bean(name = ["cmpPar8PartitionHandler"])
        fun par8PartitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 8))
            gridSize = 8
        }

        @Bean(name = ["cmpPar8JobOperatorTestUtils"])
        fun par8JobOperatorTestUtils(
            @Qualifier("cmpPar8Job") job: Job,
            jobOperator: JobOperator,
        ): JobOperatorTestUtils = JobOperatorTestUtils(jobOperator, jobRepository).apply {
            this.job = job
        }
    }

    @Autowired @Qualifier("cmpSeqJobOperatorTestUtils")
    private lateinit var seqUtils: JobOperatorTestUtils

    @Autowired @Qualifier("cmpPar4JobOperatorTestUtils")
    private lateinit var par4Utils: JobOperatorTestUtils

    @Autowired @Qualifier("cmpPar8JobOperatorTestUtils")
    private lateinit var par8Utils: JobOperatorTestUtils

    @Test
    @Order(1)
    fun `1파티션 순차 처리 시간 측정`() {
        insertTestData(50_000)

        val elapsed = measureTimeMillis {
            val execution = seqUtils.startJob(uniqueParams())
            execution.status shouldBeEqualTo BatchStatus.COMPLETED
        }
        log.info { "[비교] 1파티션 순차:  ${elapsed}ms (50,000건)" }
    }

    @Test
    @Order(2)
    fun `4파티션 VirtualThread 처리 시간 측정`() {
        insertTestData(50_000)

        val elapsed = measureTimeMillis {
            val execution = par4Utils.startJob(uniqueParams())
            execution.status shouldBeEqualTo BatchStatus.COMPLETED
        }
        log.info { "[비교] 4파티션 병렬:  ${elapsed}ms (50,000건)" }
    }

    @Test
    @Order(3)
    fun `8파티션 VirtualThread 처리 시간 측정`() {
        insertTestData(50_000)

        val elapsed = measureTimeMillis {
            val execution = par8Utils.startJob(uniqueParams())
            execution.status shouldBeEqualTo BatchStatus.COMPLETED
        }
        log.info { "[비교] 8파티션 병렬:  ${elapsed}ms (50,000건)" }
    }

    private fun uniqueParams() = JobParametersBuilder()
        .addLong("run.id", System.currentTimeMillis())
        .toJobParameters()
}
