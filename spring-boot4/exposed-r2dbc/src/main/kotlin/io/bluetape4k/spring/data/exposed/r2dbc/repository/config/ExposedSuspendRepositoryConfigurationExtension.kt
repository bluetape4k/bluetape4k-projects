package io.bluetape4k.spring.data.exposed.r2dbc.repository.config

import io.bluetape4k.spring.data.exposed.jdbc.annotation.ExposedEntity
import io.bluetape4k.spring.data.exposed.r2dbc.repository.ExposedR2dbcRepository
import io.bluetape4k.spring.data.exposed.r2dbc.repository.support.ExposedR2dbcRepositoryFactoryBean
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport
import org.springframework.data.repository.core.RepositoryMetadata

/**
 * suspend Exposed Spring Data 모듈 설정 확장입니다.
 *
 * ```kotlin
 * // Spring Data 내부 인프라에서 사용됩니다. 직접 인스턴스화할 필요 없습니다.
 * // @EnableExposedR2dbcRepositories 어노테이션이 자동으로 이 클래스를 사용합니다.
 * // 모듈 이름: "SUSPEND_EXPOSED", Factory Bean: ExposedR2dbcRepositoryFactoryBean
 * ```
 */
class ExposedSuspendRepositoryConfigurationExtension : RepositoryConfigurationExtensionSupport() {

    override fun getModuleName(): String = "SUSPEND_EXPOSED"

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getModulePrefix(): String = "suspendExposed"

    override fun getRepositoryFactoryBeanClassName(): String =
        ExposedR2dbcRepositoryFactoryBean::class.java.name

    override fun getIdentifyingAnnotations(): Collection<Class<out Annotation>> =
        listOf(ExposedEntity::class.java)

    override fun getIdentifyingTypes(): Collection<Class<*>> =
        listOf(ExposedR2dbcRepository::class.java)

    /**
     * 코루틴/Flow 기반의 reactive repository를 지원합니다.
     * Spring Data의 reactive 체크를 우회하여 suspend/Flow 메서드를 포함한 모든 repository를 허용합니다.
     */
    override fun useRepositoryConfiguration(metadata: RepositoryMetadata): Boolean = true
}
