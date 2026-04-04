package io.bluetape4k.rule.annotation

import java.lang.annotation.Inherited

/**
 * Rule의 조건을 검사하는 메서드를 나타내는 어노테이션입니다.
 * Boolean 반환 타입이어야 합니다.
 */
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@MustBeDocumented
annotation class Condition
