@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import kotlin.enums.EnumEntries

/**
 * Enum 정보를 name to enum value 의 map으로 빌드합니다.
 *
 * ```kotlin
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
 * ```kotlin
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
 * ```kotlin
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
 * ```kotlin
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
