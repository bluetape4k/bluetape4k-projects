package io.bluetape4k.spring.data.exposed.jdbc.repository.config

import io.bluetape4k.spring.data.exposed.jdbc.annotation.ExposedEntity
import io.bluetape4k.spring.data.exposed.jdbc.repository.ExposedJdbcRepository
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.ExposedJdbcRepositoryFactoryBean
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport

/**
 * Exposed Spring Data 모듈 설정 확장입니다.
 *
 * `@EnableExposedJdbcRepositories`가 활성화될 때 Spring Data 인프라가 이 클래스를 사용하여
 * Repository 빈 등록, 엔티티 식별 어노테이션, 기본 구현 클래스를 결정합니다.
 *
 * ```kotlin
 * // 내부적으로 자동 사용됩니다. 직접 인스턴스화할 필요는 없습니다.
 * val ext = ExposedJdbcRepositoryConfigurationExtension()
 * ext.getModuleName()  // "EXPOSED"
 * ```
 */
class ExposedJdbcRepositoryConfigurationExtension: RepositoryConfigurationExtensionSupport() {

    override fun getModuleName(): String = "EXPOSED"

    @Deprecated("use getModuleName instead.")
    override fun getModulePrefix(): String = "exposed"

    override fun getRepositoryFactoryBeanClassName(): String =
        ExposedJdbcRepositoryFactoryBean::class.java.name

    override fun getIdentifyingAnnotations(): Collection<Class<out Annotation>> =
        listOf(ExposedEntity::class.java)

    override fun getIdentifyingTypes(): Collection<Class<*>> =
        listOf(ExposedJdbcRepository::class.java)
}
