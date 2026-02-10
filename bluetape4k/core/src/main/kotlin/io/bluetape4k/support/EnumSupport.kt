@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import kotlin.enums.EnumEntries
import kotlin.reflect.KClass

/**
 * Enum 정보를 name to enum value 의 map으로 빌드합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 * val map = enumEntries<Color>().enumMap() // { "RED" to Color.RED, "GREEN" to Color.GREEN, "BLUE" to Color.BLUE }
 * ```
 *
 * @return Enum 정보의 Map
 */
inline fun <E: Enum<E>> EnumEntries<E>.enumMap(): Map<String, E> = associateBy { it.name }

/**
 * Enum 값들을 List로 반환합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 * val list = enumEntries<Color>().enumList() // [Color.RED, Color.GREEN, Color.BLUE]
 * ```
 *
 * @return Enum 값들의 List
 */
inline fun <E: Enum<E>> EnumEntries<E>.enumList(): List<E> = toList()

/**
 * enum name 값으로 Enum 을 찾습니다. 없으면 null 을 반환합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 *
 * enumEntries<Color>().findByNameOrNull("BLUE")  // Color.BLUE
 * enumEntries<Color>().findByNameOrNull("blue")  // null
 *
 * enumEntries<Color>().findByNameOrNull("BLUE", ignoreCase=true)  // Color.BLUE
 * enumEntries<Color>().findByNameOrNull("blue", ignoreCase=true)  // Color.BLUE
 * ```
 */
inline fun <E: Enum<E>> EnumEntries<E>.findByNameOrNull(name: String, ignoreCase: Boolean = false): E? =
    this.find { it.name.equals(name, ignoreCase) }

/**
 * enum name 값으로 Enum 을 찾습니다. 없으면 null 을 반환합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 *
 * enumEntries<Color>().isValidName("BLUE")  // true
 * enumEntries<Color>().isValidName("blue")  // false
 *
 * enumEntries<Color>().isValidName("BLUE", ignoreCase=true)  // true
 * enumEntries<Color>().isValidName("blue", ignoreCase=true)  // true
 * ```
 */
inline fun <E: Enum<E>> EnumEntries<E>.isValidName(name: String, ignoreCase: Boolean = false): Boolean =
    findByNameOrNull(name, ignoreCase) != null


/**
 * Enum 정보를 name to enum value 의 map으로 빌드합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 * val map = Color::class.java.enumMap() // { "RED" to Color.RED, "GREEN" to Color.GREEN, "BLUE" to Color.BLUE }
 * ```
 *
 * @return Enum 정보의 Map
 */
fun <E: Enum<E>> Class<E>.enumMap(): Map<String, E> =
    this.enumConstants.associateBy { it.name }

/**
 * Enum 정보를 name to enum value 의 map으로 빌드합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 * val map = Color::class.enumMap() // { "RED" to Color.RED, "GREEN" to Color.GREEN, "BLUE" to Color.BLUE }
 * ```
 *
 * @return Enum 정보의 Map
 */
fun <E: Enum<E>> KClass<E>.enumMap(): Map<String, E> = java.enumMap()


/**
 * Enum 값들을 List로 반환합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 * val list = Color::class.java.enumList() // [Color.RED, Color.GREEN, Color.BLUE]
 * ```
 *
 * @return Enum 값들의 List
 */
fun <E: Enum<E>> Class<E>.enumList(): List<E> = this.enumConstants.toList()

/**
 * Enum 값들을 List로 반환합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 * val list = Color::class.enumList() // [Color.RED, Color.GREEN, Color.BLUE]
 * ```
 *
 * @return Enum 값들의 List
 */
fun <E: Enum<E>> KClass<E>.enumList(): List<E> = java.enumList()

/**
 * Enum 값을 [name]으로 검색합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 * Color::class.java.getByName("BLUE") // Color.BLUE
 * Color::class.java.getByName("Blue", ignoreCase = true) // Color.BLUE
 * Color::class.java.getByName("BLACK") // null
 * ```
 *
 * @param name 검색할 이름
 * @param ignoreCase 대소문자 무시 여부 (기본값: false)
 * @return 검색된 Enum 값 (존재하지 않을 경우 null)
 */
fun <E: Enum<E>> Class<E>.findByNameOrNull(name: String, ignoreCase: Boolean = false): E? =
    this.enumConstants.firstOrNull { it.name.equals(name, ignoreCase) }

/**
 * Enum 값을 [name]으로 검색합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 * Color::class.getByName("BLUE") // Color.BLUE
 * Color::class.getByName("Blue", ignoreCase = true) // Color.BLUE
 * Color::class.getByName("BLACK") // null
 * ```
 *
 * @param name 검색할 이름
 * @param ignoreCase 대소문자 무시 여부 (기본값: false)
 * @return 검색된 Enum 값 (존재하지 않을 경우 null)
 */
fun <E: Enum<E>> KClass<E>.findByNameOrNull(name: String, ignoreCase: Boolean = false): E? =
    java.findByNameOrNull(name, ignoreCase)


/**
 * Enum 값 중에 [name]을 가지는 값이 존재하는지 검색합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 * Color::class.java.isValidName("BLUE") // true
 * Color::class.java.isValidName("Blue", ignoreCase = true) // true
 * Color::class.java.isValidName("BLACK") // false
 * ```
 *
 * @param name 검색할 이름
 * @param ignoreCase 대소문자 무시 여부 (기본값: false)
 * @return 존재 여부
 */
fun <E: Enum<E>> Class<E>.isValidName(name: String, ignoreCase: Boolean = false): Boolean =
    findByNameOrNull(name, ignoreCase) != null

/**
 * Enum 값 중에 [name]을 가지는 값이 존재하는지 검색합니다.
 *
 * ```
 * enum class Color { RED, GREEN, BLUE }
 * Color::class.isValidName("BLUE") // true
 * Color::class.isValidName("Blue", ignoreCase = true) // true
 * Color::class.isValidName("BLACK") // false
 * ```
 *
 * @param name 검색할 이름
 * @param ignoreCase 대소문자 무시 여부 (기본값: false)
 * @return 존재 여부
 */
fun <E: Enum<E>> KClass<E>.isValidName(name: String, ignoreCase: Boolean = false): Boolean =
    java.isValidName(name, ignoreCase)
