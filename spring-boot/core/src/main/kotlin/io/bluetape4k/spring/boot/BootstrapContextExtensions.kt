package io.bluetape4k.spring.boot

import org.springframework.boot.bootstrap.BootstrapContext
import java.util.function.Supplier

/** 등록된 타입의 값을 반환합니다. 등록되지 않았으면 [IllegalStateException]을 던지며, 값 자체는 null일 수 있습니다. */
inline fun <reified T : Any> BootstrapContext.get(): T? =
    get(T::class.java)

/** 등록된 값을 반환하고, 미등록이면 전달된 기본값을 반환합니다. 기본값 인자는 이 함수 호출 전에 평가됩니다. */
inline fun <reified T : Any> BootstrapContext.getOrElse(defaultValue: T?): T? =
    getOrElse(T::class.java, defaultValue)

/** 등록된 값을 반환하고, 미등록이면 [other]를 호출해 기본값을 얻습니다. */
inline fun <reified T : Any> BootstrapContext.getOrElseSupply(other: Supplier<T?>): T? =
    getOrElseSupply(T::class.java, other)

/** 등록된 값을 반환하고, 미등록이면 [exceptionSupplier]가 만든 예외를 던집니다. */
inline fun <reified T : Any> BootstrapContext.getOrElseThrow(exceptionSupplier: Supplier<out Throwable>): T? =
    getOrElseThrow(T::class.java, exceptionSupplier)

/** 타입이 등록되었는지 반환합니다. 등록된 supplier가 null을 반환하는지는 검사하지 않습니다. */
inline fun <reified T : Any> BootstrapContext.isRegistered(): Boolean =
    isRegistered(T::class.java)
