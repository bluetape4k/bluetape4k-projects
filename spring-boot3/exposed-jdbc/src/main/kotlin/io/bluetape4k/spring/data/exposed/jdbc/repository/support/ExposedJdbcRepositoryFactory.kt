package io.bluetape4k.spring.data.exposed.jdbc.repository.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.repository.query.ExposedQueryLookupStrategy
import io.bluetape4k.support.toOptional
import org.jetbrains.exposed.v1.dao.Entity
import org.springframework.data.repository.core.EntityInformation
import org.springframework.data.repository.core.RepositoryInformation
import org.springframework.data.repository.core.RepositoryMetadata
import org.springframework.data.repository.core.support.RepositoryFactorySupport
import org.springframework.data.repository.query.QueryLookupStrategy
import org.springframework.data.repository.query.ValueExpressionDelegate
import java.util.*

/**
 * Exposed Repository 인스턴스를 생성하는 Factory입니다.
 *
 * Spring Data 인프라가 `@EnableExposedJdbcRepositories` 스캔 시 자동으로 사용하며,
 * [SimpleExposedJdbcRepository] 인스턴스와 [ExposedQueryLookupStrategy]를 설정합니다.
 *
 * ```kotlin
 * // 직접 사용 시 (주로 테스트)
 * val factory = ExposedJdbcRepositoryFactory()
 * val userRepo = factory.getRepository(UserRepository::class.java)
 * ```
 */
@Suppress("UNCHECKED_CAST")
class ExposedJdbcRepositoryFactory: RepositoryFactorySupport() {

    companion object: KLogging()

    override fun <T: Any, ID: Any> getEntityInformation(domainClass: Class<T>): EntityInformation<T, ID> =
        ExposedEntityInformationImpl(domainClass as Class<Entity<Any>>) as EntityInformation<T, ID>

    override fun getTargetRepository(information: RepositoryInformation): Any {
        val entityInfo = exposedEntityInformation(information.domainType)
        return SimpleExposedJdbcRepository(entityInfo)
    }

    override fun getRepositoryBaseClass(metadata: RepositoryMetadata): Class<*> =
        SimpleExposedJdbcRepository::class.java

    override fun getQueryLookupStrategy(
        key: QueryLookupStrategy.Key?,
        valueExpressionDelegate: ValueExpressionDelegate,
    ): Optional<QueryLookupStrategy> =
        ExposedQueryLookupStrategy.create(key ?: QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND).toOptional()

    private fun exposedEntityInformation(domainClass: Class<*>): ExposedEntityInformation<Entity<Any>, Any> =
        ExposedEntityInformationImpl(domainClass as Class<Entity<Any>>) as ExposedEntityInformation<Entity<Any>, Any>
}
