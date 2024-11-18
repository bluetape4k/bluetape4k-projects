package io.bluetape4k.resilience4j

import kotlin.reflect.KClass

inline fun <T, R> (() -> T).andThen(
    crossinline resultHandler: (result: T) -> R,
): () -> R = {
    resultHandler.invoke(this.invoke())
}

inline fun <T, R> (() -> T).andThen(
    crossinline handler: (result: T?, error: Throwable?) -> R,
): () -> R = {
    try {
        val result = this.invoke()
        handler.invoke(result, null)
    } catch (e: Exception) {
        handler.invoke(null, e)
    }
}

inline fun <T, R> (() -> T).andThen(
    crossinline resultHandler: (T) -> R,
    crossinline exceptionHandler: (Throwable?) -> R,
): () -> R = {
    try {
        val result = this.invoke()
        resultHandler(result)
    } catch (e: Exception) {
        exceptionHandler.invoke(e)
    }
}

/**
 * `Provider` 실행 시 예외가 발생하면 데체 값을 제공하도록 합니다.
 *
 * @receiver (()->T)
 * @param exceptionHandler Function1<[@kotlin.ParameterName] Exception, T>
 * @return (()->T)
 */
@JvmName("recoverWithExceptionHandler")
inline fun <T> (() -> T).recover(crossinline exceptionHandler: (Throwable?) -> T): () -> T = {
    try {
        this.invoke()
    } catch (e: Exception) {
        exceptionHandler.invoke(e)
    }
}

@JvmName("recoverWithResultHandler")
inline fun <T> (() -> T).recover(
    crossinline resultPredicatoe: (T) -> Boolean,
    crossinline resultHandler: (T) -> T,
): () -> T = {
    val result = this.invoke()

    if (resultPredicatoe.invoke(result)) {
        resultHandler.invoke(result)
    } else {
        result
    }
}

@JvmName("recoverWithExceptionType")
inline fun <X: Throwable, T> (() -> T).recover(
    exceptionType: KClass<X>,
    crossinline exceptionHandler: (Throwable?) -> T,
): () -> T = {
    try {
        this.invoke()
    } catch (e: Throwable) {
        if (exceptionType.java.isAssignableFrom(e.javaClass)) {
            exceptionHandler.invoke(e)
        } else {
            throw e
        }
    }
}

@JvmName("recoverWithExceptionTypes")
inline fun <T> (() -> T).recover(
    exceptionTypes: Iterable<Class<out Throwable>>,
    crossinline exceptionHandler: (Throwable?) -> T,
): () -> T = {
    try {
        this.invoke()
    } catch (e: Throwable) {
        if (exceptionTypes.any { it.isAssignableFrom(e.javaClass) }) {
            exceptionHandler.invoke(e)
        } else {
            throw e
        }
    }
}

inline fun <T> (() -> T).recover(
    crossinline resultHandler: (T) -> T,
    crossinline exceptionHandler: (Throwable?) -> T,
): () -> T = {
    try {
        val result = this.invoke()
        resultHandler(result)
    } catch (e: Throwable) {
        exceptionHandler.invoke(e)
    }
}
