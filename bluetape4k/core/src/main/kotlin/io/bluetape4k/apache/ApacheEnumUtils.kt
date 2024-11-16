package io.bluetape4k.apache

import org.apache.commons.lang3.EnumUtils
import kotlin.reflect.KClass

/**
 * Gets the enum for the class with the given name.
 *
 * @receiver the class to get the enum for
 * @param enumName the name of the enum to get
 * @return the enum for the class
 */
fun <E: Enum<E>> KClass<E>.getEnumIgnoreCase(enumName: String): E? =
    EnumUtils.getEnumIgnoreCase(this.java, enumName)

/**
 * Gets the enum for the class with the given name.
 *
 * @receiver the class to get the enum for
 * @param enumName the name of the enum to get
 * @param defaultEnum the default enum to return if the enum is not found
 * @return the enum for the class
 */
fun <E: Enum<E>> KClass<E>.getEnumIgnoreCase(enumName: String, defaultEnum: E): E? =
    EnumUtils.getEnumIgnoreCase(this.java, enumName, defaultEnum)

/**
 *  Gets the enum for the class with the given name.
 */
fun <E: Enum<E>> KClass<E>.getEnumList(): List<E> = EnumUtils.getEnumList(this.java)

/**
 * Gets the enum for the class with the given name.
 */
fun <E: Enum<E>> KClass<E>.getEnumMap(): Map<String, E> = EnumUtils.getEnumMap(this.java)

/**
 * Gets the enum for the class with the given name.
 */
fun <E: Enum<E>, K> KClass<E>.getEnumMap(keySelector: (E) -> K): Map<K, E> =
    EnumUtils.getEnumMap(this.java, keySelector)

/**
 * Gets the enum for the class with the given name.
 */
fun <E: Enum<E>> KClass<E>.isValidEnum(enumName: String): Boolean =
    EnumUtils.isValidEnum(this.java, enumName)

/**
 * Gets the enum for the class with the given name.
 */
fun <E: Enum<E>> KClass<E>.isValidEnumIgnoreCase(enumName: String): Boolean =
    EnumUtils.isValidEnumIgnoreCase(this.java, enumName)
