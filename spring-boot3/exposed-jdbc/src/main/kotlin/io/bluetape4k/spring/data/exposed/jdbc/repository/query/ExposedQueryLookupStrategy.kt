package io.bluetape4k.spring.data.exposed.jdbc.repository.query

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.ExposedEntityInformationImpl
import org.jetbrains.exposed.v1.dao.Entity
import org.springframework.data.projection.ProjectionFactory
import org.springframework.data.repository.core.NamedQueries
import org.springframework.data.repository.core.RepositoryMetadata
import org.springframework.data.repository.query.QueryLookupStrategy
import org.springframework.data.repository.query.RepositoryQuery
import java.lang.reflect.Method

/**
 * Exposed Repository 메서드의 쿼리 전략을 결정합니다.
 *
 * - [QueryLookupStrategy.Key.USE_DECLARED_QUERY]: `@Query` 어노테이션 쿼리 사용
 * - [QueryLookupStrategy.Key.CREATE]: 메서드명에서 PartTree 쿼리 생성
 * - [QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND]: `@Query` 먼저, 없으면 메서드명 파생
 *
 * ```kotlin
 * // ExposedJdbcRepositoryFactory 내부에서 자동 생성됩니다.
 * val strategy = ExposedQueryLookupStrategy.create(
 *     QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND
 * )
 * // @Query 어노테이션이 있으면 DeclaredExposedQuery,
 * // 없으면 메서드명 기반 PartTreeExposedQuery를 반환합니다.
 * ```
 */
class ExposedQueryLookupStrategy(
    private val key: QueryLookupStrategy.Key,
): QueryLookupStrategy {

    companion object: KLogging() {
        fun create(key: QueryLookupStrategy.Key): ExposedQueryLookupStrategy = ExposedQueryLookupStrategy(key)
    }

    override fun resolveQuery(
        method: Method,
        metadata: RepositoryMetadata,
        factory: ProjectionFactory,
        namedQueries: NamedQueries,
    ): RepositoryQuery {
        val queryMethod = ExposedQueryMethod(method, metadata, factory)

        @Suppress("UNCHECKED_CAST")
        val entityInformation =
            ExposedEntityInformationImpl(
                metadata.domainType as Class<Entity<Any>>,
            )

        return when (key) {
            QueryLookupStrategy.Key.USE_DECLARED_QUERY -> {
                require(queryMethod.isAnnotatedQuery) {
                    "No @Query annotation found on method '${method.name}'"
                }
                DeclaredExposedQuery(queryMethod, entityInformation)
            }

            QueryLookupStrategy.Key.CREATE             -> {
                PartTreeExposedQuery(queryMethod, entityInformation)
            }

            QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND -> {
                if (queryMethod.isAnnotatedQuery) {
                    DeclaredExposedQuery(queryMethod, entityInformation)
                } else {
                    PartTreeExposedQuery(queryMethod, entityInformation)
                }
            }
        }
    }
}
