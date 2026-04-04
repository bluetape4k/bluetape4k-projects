package io.bluetape4k.rule.annotation

import java.lang.annotation.Inherited

/**
 * Rule에서 실행할 작업을 나타내는 어노테이션입니다.
 *
 * ```kotlin
 * @Rule(name = "myRule")
 * class MyRule {
 *     @Condition
 *     fun check(facts: Facts): Boolean = facts.get<Int>("value")!! > 0
 *
 *     @Action(order = 1)
 *     fun firstAction(facts: Facts) { facts["step1"] = true }
 *
 *     @Action(order = 2)
 *     fun secondAction(facts: Facts) { facts["step2"] = true }
 * }
 * ```
 *
 * @property order 액션 실행 순서 (낮을수록 먼저 실행)
 */
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@MustBeDocumented
annotation class Action(val order: Int = 0)
