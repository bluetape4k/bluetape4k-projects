package io.bluetape4k.apache

import org.apache.commons.lang3.ClassUtils
import kotlin.reflect.KClass

/**
 * 클래스의 축약된 이름을 반환합니다.
 *
 * 축약된 이름은 패키지 이름이 제외된 클래스 이름을 의미합니다.
 *
 * @receiver 축약 이름을 가져올 클래스
 * @return 축약된 클래스 이름
 */
fun KClass<*>.getAbbrName(): String = ClassUtils.getAbbreviatedName(this.java, 0)

/**
 * 클래스 이름 문자열의 축약된 이름을 반환합니다.
 *
 * 축약된 이름은 패키지 이름이 제외된 클래스 이름을 의미합니다.
 *
 * @param className 축약 이름을 가져올 클래스 이름 문자열
 * @return 축약된 클래스 이름
 */
fun getAbbrName(className: String): String = ClassUtils.getAbbreviatedName(className, 0)

/**
 * 해당 클래스가 구현하는 모든 인터페이스 목록을 반환합니다.
 *
 * @receiver 인터페이스 목록을 가져올 클래스
 * @return 구현된 인터페이스 목록
 */
fun KClass<*>.getAllInterfaces(): List<Class<*>> = ClassUtils.getAllInterfaces(this.java)

/**
 * 모든 상위 클래스 목록을 반환합니다.
 *
 * @receiver 상위 클래스 목록을 가져올 클래스
 * @return 상위 클래스 목록
 */
fun KClass<*>.getAllSuperclasses(): List<Class<*>> = ClassUtils.getAllSuperclasses(this.java)

/**
 * [KClass]의 정규 이름(canonical name)을 반환합니다.
 *
 * @receiver 정규 이름을 가져올 클래스
 * @return 클래스의 정규 이름
 */
fun KClass<*>.getCanonicalName(): String = ClassUtils.getCanonicalName(this.java)

/**
 * 클래스 이름에서 패키지의 정규 이름을 반환합니다.
 *
 * 입력 문자열은 클래스 이름으로 간주하며 별도로 검증하지 않습니다.
 * 기본 패키지에 속하는 경우 빈 문자열을 반환합니다.
 *
 * @receiver 패키지 이름을 가져올 클래스
 * @return 패키지 이름 또는 빈 문자열
 */
fun KClass<*>.getPackageCanonicalName(): String = ClassUtils.getPackageCanonicalName(this.java)

/**
 * [Class]의 패키지 이름을 반환합니다.
 *
 * @receiver 패키지 이름을 가져올 클래스
 * @return 패키지 이름 또는 빈 문자열
 */
fun KClass<*>.getPackageName(): String = ClassUtils.getPackageName(this.java)

/**
 * [Class]에서 패키지 이름을 제외한 짧은 정규 이름을 반환합니다.
 *
 * @receiver 짧은 정규 이름을 가져올 클래스
 * @return 패키지 이름이 제외된 정규 이름 또는 빈 문자열
 */
fun KClass<*>.getShortCanonicalName(): String =
    ClassUtils.getShortCanonicalName(this.java)

/**
 * [Class]에서 패키지 이름을 제외한 짧은 클래스 이름을 반환합니다.
 *
 * 내부적으로 `Class.getName()`으로 이름을 가져온 뒤 패키지를 제거합니다.
 * 내부 클래스인 경우 외부 클래스 이름이 `.`(점)으로 구분되어 포함됩니다.
 *
 * @receiver 짧은 클래스 이름을 가져올 클래스
 * @return 패키지 이름이 제외된 클래스 이름 또는 빈 문자열
 */
fun KClass<*>.getShortClassName(): String =
    ClassUtils.getShortClassName(this.java)

/**
 * hierarchy 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val names = String::class.hierarchy().map { it.simpleName }
 * // names.first() == "String"
 * ```
 */
fun KClass<*>.hierarchy(): Iterable<Class<*>> =
    ClassUtils.hierarchy(this.java)

/**
 * 클래스 계층 구조를 서브클래스에서 슈퍼클래스 방향(오름차순)으로 순회하는 [Iterable]을 반환합니다.
 *
 * @param interfaceBehavior 인터페이스 포함 여부를 지정하는 옵션
 * @return 클래스 계층 구조를 순회하는 [Iterable]
 */
fun KClass<*>.hierarchy(interfaceBehavior: ClassUtils.Interfaces): Iterable<Class<*>> =
    ClassUtils.hierarchy(this.java, interfaceBehavior)


/**
 * isAssignable 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = String::class.isAssignable(CharSequence::class)
 * // result == true
 * ```
 */
fun KClass<*>.isAssignable(toClass: KClass<*>, autoboxing: Boolean = true): Boolean =
    ClassUtils.isAssignable(this.java, toClass.java, autoboxing)

/**
 * Is the specified class an inner class or static nested class?
 *
 * @receiver the class to check may be null
 * @return {@code true} if the class is an inner or static nested class, false if not or `null`
 */
fun KClass<*>.isInnerClass(): Boolean = ClassUtils.isInnerClass(this.java)
