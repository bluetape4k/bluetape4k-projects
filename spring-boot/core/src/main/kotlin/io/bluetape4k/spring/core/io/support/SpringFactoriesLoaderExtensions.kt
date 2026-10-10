package io.bluetape4k.spring.core.io.support

import org.springframework.core.io.support.SpringFactoriesLoader

/** receiver가 가진 resource와 classloader를 사용해 reified factory 타입을 로드합니다. */
inline fun <reified T : Any> SpringFactoriesLoader.load(
    argumentResolver: SpringFactoriesLoader.ArgumentResolver? = null,
    failureHandler: SpringFactoriesLoader.FailureHandler? = null,
): List<T> = load(T::class.java, argumentResolver, failureHandler)
