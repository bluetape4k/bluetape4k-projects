package io.bluetape4k.spring.data.exposed.jdbc.repository.support

import org.springframework.data.repository.Repository
import org.springframework.data.repository.core.support.RepositoryFactorySupport
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport

/**
 * Exposed Repository Bean을 생성하는 FactoryBean입니다.
 * Spring의 트랜잭션 관리와 통합됩니다.
 *
 * `@EnableExposedJdbcRepositories`의 `repositoryFactoryBeanClass`로 등록됩니다.
 * Spring Data가 Repository 인터페이스별로 이 FactoryBean을 생성합니다.
 *
 * ```kotlin
 * // Spring Boot 자동 설정 흐름:
 * // @EnableExposedJdbcRepositories
 * //   → ExposedJdbcRepositoriesRegistrar
 * //   → ExposedJdbcRepositoryFactoryBean (각 Repository 인터페이스별 빈 생성)
 * //   → ExposedJdbcRepositoryFactory.getRepository(UserRepository::class.java)
 * ```
 */
class ExposedJdbcRepositoryFactoryBean<T: Repository<E, ID>, E: Any, ID: Any>(
    repositoryInterface: Class<out T>,
): TransactionalRepositoryFactoryBeanSupport<T, E, ID>(repositoryInterface) {

    override fun doCreateRepositoryFactory(): RepositoryFactorySupport =
        ExposedJdbcRepositoryFactory()
}
