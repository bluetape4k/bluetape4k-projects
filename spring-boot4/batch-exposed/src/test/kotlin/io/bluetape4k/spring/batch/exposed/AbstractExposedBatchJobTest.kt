package io.bluetape4k.spring.batch.exposed

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.spring7.transaction.SpringTransactionManager
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * Spring Boot + Spring Batch + Exposed H2 통합 테스트 베이스 클래스.
 *
 * - H2 인메모리 DB (`application-test.yml`) + Spring Batch 스키마 자동 초기화
 * - [SpringTransactionManager]로 Exposed 트랜잭션을 Spring 관리 트랜잭션에 통합
 * - [BeforeEach]에서 [SourceTable] / [TargetTable] Drop → Create
 *
 * 사용 예:
 * ```kotlin
 * class MyJobTest : AbstractExposedBatchJobTest() {
 *     @TestConfiguration
 *     class JobConfig(private val jobRepository: JobRepository, ...) {
 *         @Bean fun myJob(): Job = ...
 *     }
 *
 *     @Autowired private lateinit var jobOperatorTestUtils: JobOperatorTestUtils
 *
 *     @Test
 *     fun `job runs to completion`() { ... }
 * }
 * ```
 */
@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractExposedBatchJobTest {

    companion object : KLogging()

    /**
     * Spring Boot 앱 + 자동 구성 진입점.
     *
     * - [DataSourceTransactionManagerAutoConfiguration]: 기본 JDBC TX 매니저 생성 방지
     * - [ExposedAutoConfiguration]: 이름 충돌(`springTransactionManager`) 방지를 위해 제외
     * - `@Bean` 메서드로 [SpringTransactionManager]와 [Database]를 직접 등록:
     *   user-defined 빈은 auto-configuration 조건 평가 전에 등록되므로
     *   `BatchAutoConfiguration`이 [PlatformTransactionManager]를 인식하여 [JobRepository]를 생성함.
     */
    @SpringBootApplication(
        excludeName = [
            // Spring Boot 4에서 패키지 이동: org.springframework.boot.jdbc.autoconfigure
            "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
            "org.jetbrains.exposed.v1.spring.boot.autoconfigure.ExposedAutoConfiguration",
        ]
    )
    class TestApplication {
        @Bean
        fun exposedDatabase(dataSource: DataSource): Database =
            Database.connect(dataSource)

        @Bean
        @Primary
        fun springTransactionManager(dataSource: DataSource): PlatformTransactionManager =
            SpringTransactionManager(dataSource, DatabaseConfig {}, false)
    }

    @Autowired
    protected lateinit var database: Database

    /**
     * 각 테스트 전 [SourceTable] / [TargetTable]을 Drop → Create하여 데이터 격리를 보장합니다.
     */
    @BeforeEach
    fun setupTables() {
        transaction(database) {
            SchemaUtils.drop(TargetTable, SourceTable)
            SchemaUtils.create(SourceTable, TargetTable)
        }
    }
}
