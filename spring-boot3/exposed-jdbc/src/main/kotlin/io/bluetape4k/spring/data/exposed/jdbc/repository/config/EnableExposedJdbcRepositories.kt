package io.bluetape4k.spring.data.exposed.jdbc.repository.config

import io.bluetape4k.spring.data.exposed.jdbc.config.ExposedSpringDataAutoConfiguration
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.ExposedJdbcRepositoryFactoryBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.repository.query.QueryLookupStrategy
import kotlin.reflect.KClass

/**
 * Exposed Repository 자동 스캐닝을 활성화합니다.
 *
 * ```kotlin
 * @SpringBootApplication
 * @EnableExposedRepositories(basePackages = ["io.example.repository"])
 * class Application
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(ExposedJdbcRepositoriesRegistrar::class, ExposedSpringDataAutoConfiguration::class)
annotation class EnableExposedJdbcRepositories(
    /** basePackages 별칭 */
    vararg val value: String = [],
    /** 스캔할 base 패키지 목록 */
    val basePackages: Array<String> = [],
    /** 스캔 기준 클래스 목록 */
    val basePackageClasses: Array<KClass<*>> = [],
    /** 스캔에서 제외할 필터 */
    val excludeFilters: Array<ComponentScan.Filter> = [],
    /** 스캔에 포함할 필터 */
    val includeFilters: Array<ComponentScan.Filter> = [],
    /** 사용할 RepositoryFactoryBean 클래스 */
    val repositoryFactoryBeanClass: KClass<*> = ExposedJdbcRepositoryFactoryBean::class,
    /** 쿼리 조회 전략 */
    val queryLookupStrategy: QueryLookupStrategy.Key = QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND,
    /** 트랜잭션 매니저 빈 이름 (Exposed Spring Boot Starter 기본값) */
    val transactionManagerRef: String = "springTransactionManager",
    /** NamedQueries 위치 */
    val namedQueriesLocation: String = "",
    /** Repository 구현체 접미사 */
    val repositoryImplementationPostfix: String = "Impl",
)
