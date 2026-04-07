package io.bluetape4k.spring.data.exposed.jdbc.repository.config

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport
import org.springframework.data.repository.config.RepositoryConfigurationExtension

/**
 * `@EnableExposedRepositories` 어노테이션으로 활성화되는 Repository 빈 등록기입니다.
 *
 * Spring의 `RepositoryBeanDefinitionRegistrarSupport`를 구현하여
 * `@EnableExposedJdbcRepositories`가 선언된 패키지를 스캔하고 Exposed Repository 빈을 등록합니다.
 *
 * ```kotlin
 * // 내부적으로 @Import(ExposedJdbcRepositoriesRegistrar::class)에 의해 자동 활성화됩니다.
 * @SpringBootApplication
 * @EnableExposedJdbcRepositories(basePackages = ["io.example.repository"])
 * class Application
 * ```
 */
class ExposedJdbcRepositoriesRegistrar: RepositoryBeanDefinitionRegistrarSupport() {

    override fun getAnnotation(): Class<out Annotation> =
        EnableExposedJdbcRepositories::class.java

    override fun getExtension(): RepositoryConfigurationExtension =
        ExposedJdbcRepositoryConfigurationExtension()
}
