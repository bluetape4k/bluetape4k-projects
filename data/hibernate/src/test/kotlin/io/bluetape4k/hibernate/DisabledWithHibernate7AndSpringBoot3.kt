package io.bluetape4k.hibernate

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Hibernate ORM 7.x requires Spring Framework 7 / Spring Boot 4 for Spring ORM integration.
 * Spring Boot 3.x `SpringBeanContainer` (hibernate5 package) does not implement H7's `ManagedBean.getBeanClass()`.
 *
 * **주의**: 이 [ExecutionCondition]은 버전 감지 없이 **무조건** 테스트를 비활성화합니다.
 * `@ExtendWith`는 `@Inherited`이므로 어노테이션된 기본 클래스의 모든 서브클래스에 적용됩니다.
 *
 * Spring Boot 4 마이그레이션 완료 후 이 클래스를 실제 버전 감지 로직으로 교체하거나 제거하세요.
 *
 * TODO: Spring Boot 4 테스트 인프라 마이그레이션 완료 후 버전 조건부 비활성화로 교체 또는 제거.
 */
class DisabledWithHibernate7AndSpringBoot3 : ExecutionCondition {
    // TODO: Spring Boot 4 마이그레이션 완료 시 실제 버전 검사로 교체하거나 이 클래스를 삭제할 것.
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        ConditionEvaluationResult.disabled(
            "H7 requires Spring Boot 4 / Spring Framework 7 — " +
                "Spring Boot 3 SpringBeanContainer (hibernate5 pkg) is incompatible with H7 ManagedBean SPI. " +
                "Migrate test infrastructure to Spring Boot 4 to re-enable."
        )
}
