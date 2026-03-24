package io.bluetape4k.apache

import org.apache.commons.lang3.reflect.ConstructorUtils
import java.lang.reflect.Constructor

/**
 * 파라미터 타입에 일치하는 접근 가능한 생성자를 반환합니다.
 *
 * @param parameterTypes 생성자의 파라미터 타입 목록
 * @return 일치하는 [Constructor]
 * @deprecated [getAccessibleConstructor]를 사용하세요.
 */
@Deprecated(
    message = "Use getAccessibleConstructor",
    replaceWith = ReplaceWith("getAccessibleConstructor(*parameterTypes)")
)
fun <T> Class<T>.getAccessbleConstructor(vararg parameterTypes: Class<*>): Constructor<T> {
    return ConstructorUtils.getAccessibleConstructor(this, *parameterTypes)
}

/**
 * 파라미터 타입에 일치하는 접근 가능한 생성자를 반환합니다.
 *
 * @param parameterTypes 생성자의 파라미터 타입 목록
 * @return 일치하는 [Constructor]
 */
fun <T> Class<T>.getAccessibleConstructor(vararg parameterTypes: Class<*>): Constructor<T> {
    return ConstructorUtils.getAccessibleConstructor(this, *parameterTypes)
}

/**
 * 파라미터 타입에 가장 잘 맞는 접근 가능한 생성자를 반환합니다.
 *
 * @param parameterTypes 생성자의 파라미터 타입 목록
 * @return 가장 적합한 [Constructor]
 */
fun <T> Class<T>.getMatchingAccessibleConstructor(vararg parameterTypes: Class<*>): Constructor<T> {
    return ConstructorUtils.getMatchingAccessibleConstructor(this, *parameterTypes)
}

/**
 * 인수 타입을 추론하여 적합한 생성자를 호출해 새 인스턴스를 생성합니다.
 *
 * 생성자 시그니처는 인수 타입과 할당 호환성(assignment compatibility)으로 매칭됩니다.
 *
 * @param args 생성자 인수 배열 (null이면 빈 배열로 처리)
 * @return 생성된 [T] 인스턴스
 */
fun <T> Class<T>.invokeConstructor(vararg args: Any?): T =
    ConstructorUtils.invokeConstructor(this, *args)

/**
 * 파라미터 타입 목록으로 적합한 생성자를 선택해 새 인스턴스를 생성합니다.
 *
 * 생성자 시그니처는 파라미터 타입과 할당 호환성으로 매칭됩니다.
 *
 * @param args 생성자 인수 배열 (null이면 빈 배열로 처리)
 * @param parameterTypes 파라미터 타입 배열 (null이면 빈 배열로 처리)
 * @return 생성된 [T] 인스턴스
 */
fun <T> Class<T>.invokeConstructor(args: Array<Any?>, parameterTypes: Array<out Class<*>>): T =
    ConstructorUtils.invokeConstructor(this, args, parameterTypes)

/**
 * 인수 타입을 추론하여 정확히 일치하는 생성자를 호출해 새 인스턴스를 생성합니다.
 *
 * 생성자 시그니처는 인수 타입과 정확히 일치해야 합니다.
 *
 * @param args 생성자 인수 배열 (null이면 빈 배열로 처리)
 * @return 생성된 [T] 인스턴스
 */
fun <T> Class<T>.invokeExactConstructor(vararg args: Any?): T =
    ConstructorUtils.invokeExactConstructor(this, *args)

/**
 * 파라미터 타입 목록으로 정확히 일치하는 생성자를 선택해 새 인스턴스를 생성합니다.
 *
 * 생성자 시그니처는 파라미터 타입과 정확히 일치해야 합니다.
 *
 * @param args 생성자 인수 배열 (null이면 빈 배열로 처리)
 * @param parameterTypes 파라미터 타입 배열 (null이면 빈 배열로 처리)
 * @return 생성된 [T] 인스턴스
 *
 * @see Constructor.newInstance
 */
fun <T> Class<T>.invokeExactConstructor(args: Array<Any?>, parameterTypes: Array<out Class<*>>): T =
    ConstructorUtils.invokeExactConstructor(this, args, parameterTypes)
