package io.bluetape4k.rule.annotation

import java.lang.annotation.Inherited

/**
 * Rule의 우선순위를 지정하는 메서드를 나타내는 어노테이션입니다.
 * Int 반환 타입이어야 합니다.
 *
 * ```kotlin
 * @Rule(name = "dynamicPriority")
 * class DynamicPriorityRule {
 *     @Priority
 *     fun getPriority(): Int = 10
 *
 *     @Condition
 *     fun check(facts: Facts): Boolean = true
 *
 *     @Action
 *     fun execute(facts: Facts) { facts["done"] = true }
 * }
 * ```
 */
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@MustBeDocumented
annotation class Priority
