package io.bluetape4k.support

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
fun <E: Enum<E>> Class<E>.enumMap(): Map<String, E> =
    this.enumConstants.associateBy { it.name }

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
fun <E: Enum<E>> Class<E>.enumList(): List<E> = this.enumConstants.toList()

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
fun <E: Enum<E>> Class<E>.getByName(name: String, ignoreCase: Boolean = false): E? =
    this.enumConstants.firstOrNull { it.name.equals(name, ignoreCase) }

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
fun <E: Enum<E>> Class<E>.isValidName(name: String, ignoreCase: Boolean = false): Boolean =
    runCatching { getByName(name, ignoreCase) != null }.getOrDefault(false)
