package io.bluetape4k.spring.data.exposed.jdbc.repository.config

import io.bluetape4k.spring.data.exposed.jdbc.annotation.ExposedEntity
import io.bluetape4k.spring.data.exposed.jdbc.repository.ExposedJdbcRepository
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.ExposedJdbcRepositoryFactoryBean
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport

/**
 * Exposed Spring Data 모듈 설정 확장입니다.
 */
class ExposedJdbcRepositoryConfigurationExtension : RepositoryConfigurationExtensionSupport() {

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
