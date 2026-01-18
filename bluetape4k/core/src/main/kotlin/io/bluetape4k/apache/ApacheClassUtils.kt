package io.bluetape4k.apache

import org.apache.commons.lang3.ClassUtils
import kotlin.reflect.KClass

/**
 * Gets the abbreviated name of a class.
 *
 * The abbreviated name stands for the class name without the package name.
 *
 * @receiver the class to get the abbreviated name for
 * @return the abbreviated name
 */
fun KClass<*>.getAbbrName(): String = ClassUtils.getAbbreviatedName(this.java, 0)

/**
 * Gets the abbreviated name of a class.
 *
 * The abbreviated name stands for the class name without the package name.
 *
 * @param className the class name to get the abbreviated name for
 * @return the abbreviated name
 */
fun getAbbrName(className: String): String = ClassUtils.getAbbreviatedName(className, 0)

/**
 * Gets the interfaces for the class with the given name.
 *
 * @receiver the class to get the interfaces for
 * @return the interfaces for the class
 */
fun KClass<*>.getAllInterfaces(): List<Class<*>> = ClassUtils.getAllInterfaces(this.java)

/**
 * Gets the superclasses for the class with the given name.
 *
 * @receiver the class to get the superclasses for
 * @return the superclasses for the class
 */
fun KClass<*>.getAllSuperclasses(): List<Class<*>> = ClassUtils.getAllSuperclasses(this.java)

/**
 * Gets the canonical name for an `KClass`.
 *
 * @receiver the object for which to get the canonical class name; may be null
 * @return the canonical name of the object or {@code valueIfNull}
 */
fun KClass<*>.getCanonicalName(): String = ClassUtils.getCanonicalName(this.java)

/**
 * Gets the package name from the class name.
 *
 * The string passed in is assumed to be a class name - it is not checked.
 *
 * If the class is in the default package, return an empty string.
 *
 * @receiver the name to get the package name for, may be `null`
 * @return the package name or an empty string
 */
fun KClass<*>.getPackageCanonicalName(): String = ClassUtils.getPackageCanonicalName(this.java)

/**
 * Gets the package name of a {@link Class}.
 *
 * @receiver the class to get the package name for.
 * @return the package name or an empty string
 */
fun KClass<*>.getPackageName(): String = ClassUtils.getPackageName(this.java)

/**
 * Gets the canonical name minus the package name from a {@link Class}.
 *
 * @receiver cls the class for which to get the short canonical class name; may be null
 * @return the canonical name without the package name or an empty string
 */
fun KClass<*>.getShortCanonicalName(): String =
    ClassUtils.getShortCanonicalName(this.java)

/**
 * Gets the class name minus the package name from a {@link Class}.
 *
 * <p>
 * This method simply gets the name using {@code Class.getName()} and then calls {@link #getShortClassName(String)}. See
 * relevant notes there.
 * </p>
 *
 * @receiver  the class to get the short name for.
 * @return the class name without the package name or an empty string. If the class is an inner class then the returned
 *         value will contain the outer class or classes separated with {@code .} (dot) character.
 */
fun KClass<*>.getShortClassName(): String =
    ClassUtils.getShortClassName(this.java)

fun KClass<*>.hierarchy(): Iterable<Class<*>> =
    ClassUtils.hierarchy(this.java)

/**
 * Gets an `Iterable` that can iterate over a class hierarchy in ascending (subclass to superclass) order.
 *
 * @param interfaceBehavior switch indicating whether to include or exclude interfaces
 * @return Iterable an Iterable over the class hierarchy of the given class
 * @since 3.2
 */
fun KClass<*>.hierarchy(interfaceBehavior: ClassUtils.Interfaces): Iterable<Class<*>> =
    ClassUtils.hierarchy(this.java, interfaceBehavior)


fun KClass<*>.isAssignable(toClass: KClass<*>, autoboxing: Boolean = true): Boolean =
    ClassUtils.isAssignable(this.java, toClass.java, autoboxing)

/**
 * Is the specified class an inner class or static nested class?
 *
 * @receiver the class to check may be null
 * @return {@code true} if the class is an inner or static nested class, false if not or `null`
 */
fun KClass<*>.isInnerClass(): Boolean = ClassUtils.isInnerClass(this.java)
