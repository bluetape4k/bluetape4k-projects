package io.bluetape4k.spring.data.exposed.r2dbc.repository.support

import org.springframework.data.repository.Repository
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport
import org.springframework.data.repository.core.support.RepositoryFactorySupport

/**
 * 코루틴 Exposed Repository를 생성하는 [RepositoryFactoryBeanSupport] 구현체입니다.
 *
 * R2DBC 트랜잭션은 [SimpleExposedR2dbcRepository] 내부의 Exposed `suspendTransaction` 이
 * 직접 관리합니다. Spring 의 트랜잭션 인터셉터를 적용하지 않습니다.
 *
 * ```kotlin
 * // @EnableExposedR2dbcRepositories 어노테이션이 자동으로 사용합니다.
 * // 직접 등록 예 (커스텀 설정 시):
 * @Bean
 * fun userRepositoryFactory(): ExposedR2dbcRepositoryFactoryBean<UserR2dbcRepository, UserDto, Long> =
 *     ExposedR2dbcRepositoryFactoryBean(UserR2dbcRepository::class.java)
 * ```
 */
class ExposedR2dbcRepositoryFactoryBean<T : Repository<E, ID>, E : Any, ID : Any>(
    repositoryInterface: Class<out T>,
) : RepositoryFactoryBeanSupport<T, E, ID>(repositoryInterface) {

    override fun createRepositoryFactory(): RepositoryFactorySupport =
        ExposedR2dbcRepositoryFactory()
}
