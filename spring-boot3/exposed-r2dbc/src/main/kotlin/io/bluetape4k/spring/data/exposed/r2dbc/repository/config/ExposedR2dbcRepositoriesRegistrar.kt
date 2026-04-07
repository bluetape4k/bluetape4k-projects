package io.bluetape4k.spring.data.exposed.r2dbc.repository.config

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport
import org.springframework.data.repository.config.RepositoryConfigurationExtension

/**
 * `@EnableExposedSuspendRepositories` 어노테이션으로 활성화되는 suspend Repository 빈 등록기입니다.
 *
 * Spring의 `RepositoryBeanDefinitionRegistrarSupport`를 구현하여
 * `@EnableExposedR2dbcRepositories`가 선언된 패키지를 스캔하고 suspend Exposed Repository 빈을 등록합니다.
 *
 * ```kotlin
 * // @Import(ExposedR2dbcRepositoriesRegistrar::class)에 의해 자동 활성화됩니다.
 * @SpringBootApplication
 * @EnableExposedR2dbcRepositories(basePackages = ["io.example.repository"])
 * class Application
 * ```
 */
class ExposedR2dbcRepositoriesRegistrar: RepositoryBeanDefinitionRegistrarSupport() {

    override fun getAnnotation(): Class<out Annotation> =
        EnableExposedR2dbcRepositories::class.java

    override fun getExtension(): RepositoryConfigurationExtension =
        ExposedSuspendRepositoryConfigurationExtension()
}
