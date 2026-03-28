package io.bluetape4k.spring.data.exposed.r2dbc.repository.config

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport
import org.springframework.data.repository.config.RepositoryConfigurationExtension

/**
 * `@EnableExposedSuspendRepositories` 어노테이션으로 활성화되는 suspend Repository 빈 등록기입니다.
 */
class ExposedR2dbcRepositoriesRegistrar : RepositoryBeanDefinitionRegistrarSupport() {

    override fun getAnnotation(): Class<out Annotation> =
        EnableExposedR2dbcRepositories::class.java

    override fun getExtension(): RepositoryConfigurationExtension =
        ExposedSuspendRepositoryConfigurationExtension()
}
