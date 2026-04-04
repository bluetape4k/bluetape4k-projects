package io.bluetape4k.rule.annotation

import java.lang.annotation.Inherited

/**
 * Rule 실행 시 주입할 Fact 데이터를 지정하는 어노테이션입니다.
 *
 * ```kotlin
 * @Rule(name = "vipCheck")
 * class VipCheckRule {
 *     @Condition
 *     fun isVip(@Fact("isVip") vip: Boolean): Boolean = vip
 *
 *     @Action
 *     fun applyDiscount(@Fact("discount") discount: Int, facts: Facts) {
 *         facts["finalDiscount"] = discount + 10
 *     }
 * }
 * ```
 *
 * @property value Fact 이름
 */
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
annotation class Fact(val value: String)
