package io.bluetape4k.apache

import org.apache.commons.lang3.EnumUtils
import kotlin.reflect.KClass

/**
 * 대소문자를 무시하고 이름에 해당하는 열거형 값을 반환합니다.
 * 일치하는 값이 없으면 null을 반환합니다.
 *
 * Apache Commons Lang3 [EnumUtils]의 Kotlin 확장 래퍼입니다.
 *
 * ```kotlin
 * enum class Color { RED, GREEN, BLUE }
 * Color::class.getEnumIgnoreCase("red")   // Color.RED
 * Color::class.getEnumIgnoreCase("GREEN") // Color.GREEN
 * Color::class.getEnumIgnoreCase("black") // null
 * ```
 *
 * @receiver 열거형 클래스
 * @param enumName 찾을 열거형 이름
 * @return 일치하는 열거형 값 또는 null
 * @see org.apache.commons.lang3.EnumUtils
 */
fun <E: Enum<E>> KClass<E>.getEnumIgnoreCase(enumName: String): E? =
    EnumUtils.getEnumIgnoreCase(this.java, enumName)

/**
 * 대소문자를 무시하고 이름에 해당하는 열거형 값을 반환합니다.
 * 일치하는 값이 없으면 [defaultEnum]을 반환합니다.
 *
 * ```kotlin
 * enum class Color { RED, GREEN, BLUE }
 * Color::class.getEnumIgnoreCase("red", Color.BLUE)   // Color.RED
 * Color::class.getEnumIgnoreCase("black", Color.BLUE) // Color.BLUE
 * ```
 *
 * @receiver 열거형 클래스
 * @param enumName 찾을 열거형 이름
 * @param defaultEnum 일치하는 값이 없을 때 반환할 기본 열거형 값
 * @return 일치하는 열거형 값 또는 [defaultEnum]
 */
fun <E: Enum<E>> KClass<E>.getEnumIgnoreCase(enumName: String, defaultEnum: E): E? =
    EnumUtils.getEnumIgnoreCase(this.java, enumName, defaultEnum)

/**
 * 열거형 클래스의 모든 값을 리스트로 반환합니다.
 *
 * ```kotlin
 * enum class Color { RED, GREEN, BLUE }
 * Color::class.getEnumList() // [Color.RED, Color.GREEN, Color.BLUE]
 * ```
 *
 * @receiver 열거형 클래스
 * @return 모든 열거형 값의 리스트
 */
fun <E: Enum<E>> KClass<E>.getEnumList(): List<E> = EnumUtils.getEnumList(this.java)

/**
 * 열거형 이름을 키로, 열거형 값을 값으로 하는 맵을 반환합니다.
 *
 * ```kotlin
 * enum class Color { RED, GREEN, BLUE }
 * Color::class.getEnumMap() // { "RED" to Color.RED, "GREEN" to Color.GREEN, "BLUE" to Color.BLUE }
 * ```
 *
 * @receiver 열거형 클래스
 * @return 이름→열거형 값의 맵
 */
fun <E: Enum<E>> KClass<E>.getEnumMap(): Map<String, E> = EnumUtils.getEnumMap(this.java)

/**
 * [keySelector]로 추출한 키를 기준으로 열거형 값을 매핑한 맵을 반환합니다.
 *
 * ```kotlin
 * enum class Color(val code: Int) { RED(1), GREEN(2), BLUE(3) }
 * Color::class.getEnumMap { it.code } // { 1 to Color.RED, 2 to Color.GREEN, 3 to Color.BLUE }
 * ```
 *
 * @receiver 열거형 클래스
 * @param keySelector 열거형 값에서 키를 추출하는 함수
 * @return 키→열거형 값의 맵
 */
fun <E: Enum<E>, K> KClass<E>.getEnumMap(keySelector: (E) -> K): Map<K, E> =
    EnumUtils.getEnumMap(this.java, keySelector)

/**
 * 지정된 이름이 유효한 열거형 값인지 여부를 반환합니다. (대소문자 구분)
 *
 * ```kotlin
 * enum class Color { RED, GREEN, BLUE }
 * Color::class.isValidEnum("RED")   // true
 * Color::class.isValidEnum("red")   // false (대소문자 구분)
 * Color::class.isValidEnum("BLACK") // false
 * ```
 *
 * @receiver 열거형 클래스
 * @param enumName 검사할 열거형 이름
 * @return 유효하면 true, 아니면 false
 */
fun <E: Enum<E>> KClass<E>.isValidEnum(enumName: String): Boolean =
    EnumUtils.isValidEnum(this.java, enumName)

/**
 * 지정된 이름이 유효한 열거형 값인지 여부를 대소문자를 무시하고 반환합니다.
 *
 * ```kotlin
 * enum class Color { RED, GREEN, BLUE }
 * Color::class.isValidEnumIgnoreCase("RED")   // true
 * Color::class.isValidEnumIgnoreCase("red")   // true (대소문자 무시)
 * Color::class.isValidEnumIgnoreCase("black") // false
 * ```
 *
 * @receiver 열거형 클래스
 * @param enumName 검사할 열거형 이름
 * @return 유효하면 true, 아니면 false
 */
fun <E: Enum<E>> KClass<E>.isValidEnumIgnoreCase(enumName: String): Boolean =
    EnumUtils.isValidEnumIgnoreCase(this.java, enumName)
