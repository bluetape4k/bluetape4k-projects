package io.bluetape4k.spring.data.exposed.r2dbc.repository.config

import io.bluetape4k.spring.data.exposed.r2dbc.repository.support.ExposedR2dbcRepositoryFactoryBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.repository.query.QueryLookupStrategy
import kotlin.reflect.KClass

/**
 * suspend 기반 Exposed Repository 스캐닝을 활성화합니다.
 *
 * ```kotlin
 * @SpringBootApplication
 * @EnableExposedSuspendRepositories(basePackages = ["io.example.repository"])
 * class Application
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(ExposedR2dbcRepositoriesRegistrar::class)
annotation class EnableExposedR2dbcRepositories(
    vararg val value: String = [],
    val basePackages: Array<String> = [],
    val basePackageClasses: Array<KClass<*>> = [],
    val excludeFilters: Array<ComponentScan.Filter> = [],
    val includeFilters: Array<ComponentScan.Filter> = [],
    val repositoryFactoryBeanClass: KClass<*> = ExposedR2dbcRepositoryFactoryBean::class,
    val queryLookupStrategy: QueryLookupStrategy.Key = QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND,
    val transactionManagerRef: String = "springTransactionManager",
    val namedQueriesLocation: String = "",
    val repositoryImplementationPostfix: String = "Impl",
)
