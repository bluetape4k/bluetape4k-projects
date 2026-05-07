package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass

/**
 * receiver가 타입 [T]의 인스턴스인지 검증한다 (reified, contract 지원).
 *
 * kotlin.contracts를 통해 smart-cast를 지원한다.
 * 이 함수 호출 이후 receiver를 [T] 타입으로 사용할 수 있다.
 *
 * null receiver는 항상 실패한다 (null is T == false).
 *
 * @receiver 검증할 값 (nullable 허용)
 * @return [T]로 캐스팅된 receiver (체이닝 지원)
 */
@OptIn(ExperimentalContracts::class)
inline fun <reified T : Any> Any?.shouldBeInstanceOf(): T {
    contract {
        returns() implies (this@shouldBeInstanceOf is T)
    }
    if (this !is T) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} to be an instance of ${T::class.qualifiedName}, " +
                "but was ${if (this == null) "<null>" else this::class.qualifiedName}."
        )
    }
    return this
}

/**
 * receiver가 타입 [T]의 인스턴스가 아닌지 검증한다 (reified).
 *
 * null receiver는 항상 통과한다 (null is T == false).
 *
 * @receiver 검증할 값 (nullable 허용)
 * @return receiver (체이닝 지원)
 */
inline fun <reified T : Any> Any?.shouldNotBeInstanceOf(): Any? {
    if (this is T) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} not to be an instance of ${T::class.qualifiedName}, " +
                "but it was."
        )
    }
    return this
}

/**
 * receiver가 [klass]의 인스턴스인지 검증한다 (KClass 버전).
 *
 * null receiver는 항상 실패한다.
 *
 * @receiver 검증할 값 (nullable 허용)
 * @param klass 기대하는 타입
 * @return receiver (체이닝 지원)
 */
infix fun Any?.shouldBeInstanceOf(klass: KClass<*>): Any? {
    if (this == null || !klass.isInstance(this)) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} to be an instance of ${klass.qualifiedName}, " +
                "but was ${if (this == null) "<null>" else this::class.qualifiedName}."
        )
    }
    return this
}

/**
 * receiver가 [klass]의 인스턴스가 아닌지 검증한다 (KClass 버전).
 *
 * null receiver는 항상 통과한다.
 *
 * @receiver 검증할 값 (nullable 허용)
 * @param klass 기대하지 않는 타입
 * @return receiver (체이닝 지원)
 */
infix fun Any?.shouldNotBeInstanceOf(klass: KClass<*>): Any? {
    if (this != null && klass.isInstance(this)) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} not to be an instance of ${klass.qualifiedName}, " +
                "but it was."
        )
    }
    return this
}
