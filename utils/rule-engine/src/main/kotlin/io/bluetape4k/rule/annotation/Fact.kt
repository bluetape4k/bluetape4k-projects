package io.bluetape4k.rule.annotation

import java.lang.annotation.Inherited

/**
 * Rule 실행 시 주입할 Fact 데이터를 지정하는 어노테이션입니다.
 *
 * @property value Fact 이름
 */
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
annotation class Fact(val value: String)
