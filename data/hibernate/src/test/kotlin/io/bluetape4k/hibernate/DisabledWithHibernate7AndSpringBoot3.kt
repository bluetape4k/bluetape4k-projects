package io.bluetape4k.hibernate

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Hibernate ORM 7.x requires Spring Framework 7 / Spring Boot 4 for Spring ORM integration.
 * Spring Boot 3.x `SpringBeanContainer` (hibernate5 package) does not implement H7's `ManagedBean.getBeanClass()`.
 *
 * This [ExecutionCondition] disables tests in classes that use Spring Boot 3 + Hibernate 7 together.
 * Since `@ExtendWith` is `@Inherited`, this applies to all subclasses of the annotated base class.
 */
class DisabledWithHibernate7AndSpringBoot3 : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        ConditionEvaluationResult.disabled(
            "H7 requires Spring Boot 4 / Spring Framework 7 — " +
                "Spring Boot 3 SpringBeanContainer (hibernate5 pkg) is incompatible with H7 ManagedBean SPI. " +
                "Migrate test infrastructure to Spring Boot 4 to re-enable."
        )
}
