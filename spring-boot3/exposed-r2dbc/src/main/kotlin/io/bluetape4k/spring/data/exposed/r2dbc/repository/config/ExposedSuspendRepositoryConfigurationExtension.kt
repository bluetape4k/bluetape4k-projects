package io.bluetape4k.spring.data.exposed.r2dbc.repository.config

import io.bluetape4k.spring.data.exposed.jdbc.annotation.ExposedEntity
import io.bluetape4k.spring.data.exposed.r2dbc.repository.ExposedR2dbcRepository
import io.bluetape4k.spring.data.exposed.r2dbc.repository.support.ExposedR2dbcRepositoryFactoryBean
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport
import org.springframework.data.repository.core.RepositoryMetadata

/**
 * suspend Exposed Spring Data 모듈 설정 확장입니다.
 *
 * `@EnableExposedR2dbcRepositories`가 활성화될 때 Spring Data 인프라가 이 클래스를 사용하여
 * suspend/Flow 기반 Repository 빈 등록, 엔티티 식별 어노테이션, 기본 구현 클래스를 결정합니다.
 *
 * ```kotlin
 * // 내부적으로 자동 사용됩니다. 직접 인스턴스화할 필요는 없습니다.
 * val ext = ExposedSuspendRepositoryConfigurationExtension()
 * ext.getModuleName()  // "SUSPEND_EXPOSED"
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
