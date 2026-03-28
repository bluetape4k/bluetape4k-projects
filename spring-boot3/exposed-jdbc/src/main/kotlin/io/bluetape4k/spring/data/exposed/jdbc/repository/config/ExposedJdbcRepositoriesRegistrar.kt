package io.bluetape4k.spring.data.exposed.jdbc.repository.config

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport
import org.springframework.data.repository.config.RepositoryConfigurationExtension

/**
 * `@EnableExposedRepositories` 어노테이션으로 활성화되는 Repository 빈 등록기입니다.
 */
class ExposedJdbcRepositoriesRegistrar : RepositoryBeanDefinitionRegistrarSupport() {

    override fun getAnnotation(): Class<out Annotation> =
        EnableExposedJdbcRepositories::class.java

    override fun getExtension(): RepositoryConfigurationExtension =
        ExposedJdbcRepositoryConfigurationExtension()
}
