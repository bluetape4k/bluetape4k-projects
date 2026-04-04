package io.bluetape4k.spring.data.exposed.jdbc.repository.config

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport
import org.springframework.data.repository.config.RepositoryConfigurationExtension

/**
 * `@EnableExposedJdbcRepositories` 어노테이션으로 활성화되는 Repository 빈 등록기입니다.
 *
 * ```kotlin
 * // @EnableExposedJdbcRepositories 어노테이션이 이 클래스를 통해 Repository를 등록합니다.
 * // 직접 사용할 일은 없으며 Spring 내부 인프라에서만 사용됩니다.
 * ```
 */
class ExposedJdbcRepositoriesRegistrar : RepositoryBeanDefinitionRegistrarSupport() {

    override fun getAnnotation(): Class<out Annotation> =
        EnableExposedJdbcRepositories::class.java

    override fun getExtension(): RepositoryConfigurationExtension =
        ExposedJdbcRepositoryConfigurationExtension()
}
