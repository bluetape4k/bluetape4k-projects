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
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import kotlin.system.measureTimeMillis

/**
 * PostgreSQL 대상 순차(1파티션) vs 병렬(4/8파티션) 처리 시간 비교 벤치마크.
 *
 * - [PostgreSQLServer.Launcher.postgres] Testcontainers 싱글턴 사용
 * - `@DynamicPropertySource`로 application-test.yml의 H2 datasource를 PostgreSQL로 교체
 * - 50,000건 기준 각 파티션 구성의 처리 시간을 로그로 출력
 *
 * CI 제외: `@Tag("benchmark")` — `./gradlew test -PexcludeTags="benchmark"`
 *
 * 로컬 실행:
 * ```bash
 * ./gradlew :bluetape4k-spring-boot3-batch-exposed:test \
 *   --tests "*PartitionComparisonPgBenchmarkTest*" -PincludeTags="benchmark"
 * ```
 */
@Tag("benchmark")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PartitionComparisonPgBenchmarkTest : AbstractExposedBatchJobTest() {

    companion object : KLogging() {
        val postgres: PostgreSQLServer by lazy { PostgreSQLServer.Launcher.postgres }

        @JvmStatic
        @DynamicPropertySource
        fun overrideDataSource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.driver-class-name") { PostgreSQLServer.DRIVER_CLASS_NAME }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }

    @TestConfiguration
    class JobConfig(
        private val jobRepository: JobRepository,
        private val transactionManager: PlatformTransactionManager,
        private val database: Database,
    ) {
        // ── 공유 Worker 컴포넌트 ─────────────────────────────────────────────

        @Bean(name = ["pgWorkerStep"])
        fun workerStep(): Step = StepBuilder("pg-migration-worker", jobRepository)
            .chunk<SourceRecord, TargetRecord>(500, transactionManager)
            .reader(keysetReader())
            .processor(ItemProcessor { source ->
                TargetRecord(sourceName = source.name.uppercase(), transformedValue = source.value * 2)
            })
            .writer(itemWriter())
            .build()

        @Bean(name = ["pgKeysetReader"])
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

        @Bean(name = ["pgItemWriter"])
        fun itemWriter(): ExposedItemWriter<TargetRecord> = ExposedItemWriter(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        // ── 1파티션 (순차) ───────────────────────────────────────────────────

        @Bean(name = ["pgSeqJob"])
        fun seqJob(): Job = partitionedBatchJob("pg-seq-job", jobRepository) {
            start(seqPartitionedStep())
        }

        @Bean(name = ["pgSeqPartitionedStep"])
        fun seqPartitionedStep(): Step = StepBuilder("pg-seq-manager", jobRepository)
            .partitioner("pg-migration-worker", seqPartitioner())
            .partitionHandler(seqPartitionHandler())
            .build()

        @Bean(name = ["pgSeqPartitioner"])
        fun seqPartitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable, gridSize = 1, database = database,
        )

        @Bean(name = ["pgSeqPartitionHandler"])
        fun seqPartitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 1))
            gridSize = 1
        }

        @Bean(name = ["pgSeqJobLauncherTestUtils"])
        fun seqJobLauncherTestUtils(
            @Qualifier("pgSeqJob") job: Job,
            jobLauncher: JobLauncher,
        ): JobLauncherTestUtils = JobLauncherTestUtils().apply {
            this.job = job
            this.jobRepository = this@JobConfig.jobRepository
            this.jobLauncher = jobLauncher
        }

        // ── 4파티션 (병렬) ───────────────────────────────────────────────────

        @Bean(name = ["pgPar4Job"])
        fun par4Job(): Job = partitionedBatchJob("pg-par4-job", jobRepository) {
            start(par4PartitionedStep())
        }

        @Bean(name = ["pgPar4PartitionedStep"])
        fun par4PartitionedStep(): Step = StepBuilder("pg-par4-manager", jobRepository)
            .partitioner("pg-migration-worker", par4Partitioner())
            .partitionHandler(par4PartitionHandler())
            .build()

        @Bean(name = ["pgPar4Partitioner"])
        fun par4Partitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable, gridSize = 4, database = database,
        )

        @Bean(name = ["pgPar4PartitionHandler"])
        fun par4PartitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 4))
            gridSize = 4
        }

        @Bean(name = ["pgPar4JobLauncherTestUtils"])
        fun par4JobLauncherTestUtils(
            @Qualifier("pgPar4Job") job: Job,
            jobLauncher: JobLauncher,
        ): JobLauncherTestUtils = JobLauncherTestUtils().apply {
            this.job = job
            this.jobRepository = this@JobConfig.jobRepository
            this.jobLauncher = jobLauncher
        }

        // ── 8파티션 (병렬) ───────────────────────────────────────────────────

        @Bean(name = ["pgPar8Job"])
        fun par8Job(): Job = partitionedBatchJob("pg-par8-job", jobRepository) {
            start(par8PartitionedStep())
        }

        @Bean(name = ["pgPar8PartitionedStep"])
        fun par8PartitionedStep(): Step = StepBuilder("pg-par8-manager", jobRepository)
            .partitioner("pg-migration-worker", par8Partitioner())
            .partitionHandler(par8PartitionHandler())
            .build()

        @Bean(name = ["pgPar8Partitioner"])
        fun par8Partitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable, gridSize = 8, database = database,
        )

        @Bean(name = ["pgPar8PartitionHandler"])
        fun par8PartitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 8))
            gridSize = 8
        }

        @Bean(name = ["pgPar8JobLauncherTestUtils"])
        fun par8JobLauncherTestUtils(
            @Qualifier("pgPar8Job") job: Job,
            jobLauncher: JobLauncher,
        ): JobLauncherTestUtils = JobLauncherTestUtils().apply {
            this.job = job
            this.jobRepository = this@JobConfig.jobRepository
            this.jobLauncher = jobLauncher
        }
    }

    @Autowired @Qualifier("pgSeqJobLauncherTestUtils")
    private lateinit var seqUtils: JobLauncherTestUtils

    @Autowired @Qualifier("pgPar4JobLauncherTestUtils")
    private lateinit var par4Utils: JobLauncherTestUtils

    @Autowired @Qualifier("pgPar8JobLauncherTestUtils")
    private lateinit var par8Utils: JobLauncherTestUtils

    @Test
    @Order(1)
    fun `PostgreSQL - 1파티션 순차 처리 시간 측정`() {
        insertTestData(50_000)

        val elapsed = measureTimeMillis {
            val execution = seqUtils.launchJob(uniqueParams())
            execution.status shouldBeEqualTo BatchStatus.COMPLETED
        }
        log.info { "[PG 비교] 1파티션 순차:  ${elapsed}ms (50,000건)" }
    }

    @Test
    @Order(2)
    fun `PostgreSQL - 4파티션 VirtualThread 처리 시간 측정`() {
        insertTestData(50_000)

        val elapsed = measureTimeMillis {
            val execution = par4Utils.launchJob(uniqueParams())
            execution.status shouldBeEqualTo BatchStatus.COMPLETED
        }
        log.info { "[PG 비교] 4파티션 병렬:  ${elapsed}ms (50,000건)" }
    }

    @Test
    @Order(3)
    fun `PostgreSQL - 8파티션 VirtualThread 처리 시간 측정`() {
        insertTestData(50_000)

        val elapsed = measureTimeMillis {
            val execution = par8Utils.launchJob(uniqueParams())
            execution.status shouldBeEqualTo BatchStatus.COMPLETED
        }
        log.info { "[PG 비교] 8파티션 병렬:  ${elapsed}ms (50,000건)" }
    }

    private fun uniqueParams() = JobParametersBuilder()
        .addLong("run.id", System.currentTimeMillis())
        .toJobParameters()
}
