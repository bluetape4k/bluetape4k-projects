package io.bluetape4k.spring.data.exposed.jdbc.repository.support

import org.springframework.data.repository.Repository
import org.springframework.data.repository.core.support.RepositoryFactorySupport
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport

/**
 * Exposed Repository Bean을 생성하는 FactoryBean입니다.
 * Spring의 트랜잭션 관리와 통합됩니다.
 *
 * ```kotlin
 * // @EnableExposedJdbcRepositories 어노테이션이 자동으로 사용합니다.
 * // 직접 등록 예 (커스텀 설정 시):
 * @Bean
 * fun userRepositoryFactory(): ExposedJdbcRepositoryFactoryBean<UserRepository, User, Long> =
 *     ExposedJdbcRepositoryFactoryBean(UserRepository::class.java)
 * ```
 */
class ExposedJdbcRepositoryFactoryBean<T : Repository<E, ID>, E : Any, ID : Any>(
    repositoryInterface: Class<out T>,
) : TransactionalRepositoryFactoryBeanSupport<T, E, ID>(repositoryInterface) {

    override fun doCreateRepositoryFactory(): RepositoryFactorySupport =
        ExposedJdbcRepositoryFactory()
}
