package io.bluetape4k.apache

import org.apache.commons.lang3.reflect.ConstructorUtils
import java.lang.reflect.Constructor

/**
 * Get a constructor of the class that matches the parameter types.
 *
 * @param parameterTypes the parameter types of the constructor
 * @return the constructor
 */
fun <T> Class<T>.getAccessbleConstructor(vararg parameterTypes: Class<*>): Constructor<T> {
    return ConstructorUtils.getAccessibleConstructor(this, *parameterTypes)
}

/**
 * Get a constructor of the class that matches the parameter types.
 *
 * @param parameterTypes the parameter types of the constructor
 * @return the constructor
 */
fun <T> Class<T>.getMatchingAccessibleConstructor(vararg parameterTypes: Class<*>): Constructor<T> {
    return ConstructorUtils.getMatchingAccessibleConstructor(this, *parameterTypes)
}

/**
 * Returns a new instance of the specified class inferring the right constructor
 * from the types of the arguments.
 *
 * <p>This locates and calls a constructor.
 * The constructor signature must match the argument types by assignment compatibility.</p>
 *
 * @param <T> the type to be constructed
 * @receiver  the class to be constructed
 * @param args  the array of arguments, `null` treated as empty
 * @return new instance of {@code cls}, not `null`
 */
fun <T> Class<T>.invokeConstructor(vararg args: Any?): T =
    ConstructorUtils.invokeConstructor(this, *args)

/**
 * Returns a new instance of the specified class choosing the right constructor
 * from the list of parameter types.
 *
 * <p>This locates and calls a constructor.
 * The constructor signature must match the parameter types by assignment compatibility.</p>
 *
 * @param <T> the type to be constructed
 * @receiver  the class to be constructed
 * @param args  the array of arguments, `null` treated as empty
 * @param parameterTypes  the array of parameter types, `null` treated as empty
 * @return new instance of {@code cls}, not `null`
 */
fun <T> Class<T>.invokeConstructor(args: Array<Any?>, parameterTypes: Array<out Class<*>>): T =
    ConstructorUtils.invokeConstructor(this, args, parameterTypes)

/**
 * Returns a new instance of the specified class inferring the right constructor
 * from the types of the arguments.
 *
 * This locates and calls a constructor.
 * The constructor signature must match the argument types exactly.
 *
 * @param <T> the type to be constructed
 * @receiver the class to be constructed, not `null`
 * @param args the array of arguments, `null` treated as empty
 * @return new instance of {@code cls}, not `null`
 *
 * @see invokeExactConstructor(Class, Object[], Class[])
 */
fun <T> Class<T>.invokeExactConstructor(vararg args: Any?): T =
    ConstructorUtils.invokeExactConstructor(this, *args)

/**
 * Returns a new instance of the specified class choosing the right constructor
 * from the list of parameter types.
 *
 * <p>This locates and calls a constructor.
 * The constructor signature must match the parameter types exactly.</p>
 *
 * @param <T> the type to be constructed
 * @receiver the class to be constructed, not `null`
 * @param args the array of arguments, `null` treated as empty
 * @param parameterTypes  the array of parameter types, `null` treated as empty
 * @return new instance of {@code cls}, not `null`
 *
 * @see Constructor#newInstance
 */
fun <T> Class<T>.invokeExactConstructor(args: Array<Any?>, parameterTypes: Array<out Class<*>>): T =
    ConstructorUtils.invokeExactConstructor(this, args, parameterTypes)
