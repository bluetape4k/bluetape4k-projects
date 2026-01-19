package io.bluetape4k.resilience4j

import kotlin.reflect.KClass

inline fun <T, R> (suspend () -> T).andThen(
    crossinline resultHandler: suspend (T) -> R,
): suspend () -> R = {
    resultHandler(this.invoke())
}

inline fun <T, R> (suspend () -> T).andThen(
    crossinline handler: suspend (T?, Throwable?) -> R,
): (suspend () -> R) = {
    try {
        val result = this()
        handler(result, null)
    } catch (e: Throwable) {
        handler(null, e)
    }
}

inline fun <T, R> (suspend () -> T).andThen(
    crossinline resultHandler: suspend (T) -> R,
    crossinline exceptionHandler: suspend (Throwable?) -> R,
): (suspend () -> R) = {
    try {
        val result = this.invoke()
        resultHandler(result)
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}


inline fun <T> (suspend () -> T).recover(
    crossinline exceptionHandler: suspend (Throwable?) -> T,
): (suspend () -> T) = {
    try {
        this.invoke()
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

inline fun <T> (suspend () -> T).recover(
    crossinline resultPredicatoe: suspend (T) -> Boolean,
    crossinline resultHandler: suspend (T) -> T,
): suspend () -> T = {
    val result = this.invoke()

    if (resultPredicatoe(result)) {
        resultHandler(result)
    } else {
        result
    }
}

inline fun <X: Throwable, T> (suspend () -> T).recover(
    exceptionType: KClass<X>,
    crossinline exceptionHandler: suspend (Throwable?) -> T,
): (suspend () -> T) = {
    try {
        this.invoke()
    } catch (e: Throwable) {
        if (exceptionType.java.isAssignableFrom(e.javaClass)) {
            exceptionHandler(e)
        } else {
            throw e
        }
    }
}

inline fun <X: Throwable, T> (suspend () -> T).recover(
    exceptionTypes: Iterable<Class<X>>,
    crossinline exceptionHandler: suspend (Throwable?) -> T,
): (suspend () -> T) = {
    try {
        this.invoke()
    } catch (e: Throwable) {
        if (exceptionTypes.any { it.isAssignableFrom(e.javaClass) }) {
            exceptionHandler(e)
        } else {
            throw e
        }
    }
}
