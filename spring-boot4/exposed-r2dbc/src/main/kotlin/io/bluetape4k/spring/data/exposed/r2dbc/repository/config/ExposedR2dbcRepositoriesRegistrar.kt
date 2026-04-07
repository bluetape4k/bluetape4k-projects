package io.bluetape4k.spring.data.exposed.r2dbc.repository.config

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport
import org.springframework.data.repository.config.RepositoryConfigurationExtension

/**
 * `@EnableExposedR2dbcRepositories` 어노테이션으로 활성화되는 suspend Repository 빈 등록기입니다.
 *
 * ```kotlin
 * // @EnableExposedR2dbcRepositories 어노테이션이 이 클래스를 통해 Repository를 등록합니다.
 * // 직접 사용할 일은 없으며 Spring 내부 인프라에서만 사용됩니다.
 * ```
 */
class ExposedR2dbcRepositoriesRegistrar: RepositoryBeanDefinitionRegistrarSupport() {

    override fun getAnnotation(): Class<out Annotation> =
        EnableExposedR2dbcRepositories::class.java

    override fun getExtension(): RepositoryConfigurationExtension =
        ExposedSuspendRepositoryConfigurationExtension()
}
