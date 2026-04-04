package io.bluetape4k.support

import kotlin.reflect.KClass

/**
 * [clazz]의 ClassLoader를 반환합니다.
 * bootstrap ClassLoader로 로드된 클래스의 경우 context ClassLoader → system ClassLoader 순으로 fallback합니다.
 *
 * ```kotlin
 * val cl = getClassLoader(String::class.java)
 * val resource = cl.getResourceAsStream("config.properties")
 * ```
 */
fun getClassLoader(clazz: Class<*>): ClassLoader =
    clazz.classLoader ?: Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()

/**
 * [kclass]의 ClassLoader를 반환합니다.
 *
 * ```kotlin
 * val cl = getClassLoader(MyService::class)
 * ```
 */
fun getClassLoader(kclass: KClass<*>): ClassLoader = getClassLoader(kclass.java)

/**
 * [T] 타입의 ClassLoader를 반환합니다.
 *
 * ```kotlin
 * val cl = getClassLoader<MyService>()
 * ```
 */
inline fun <reified T> getClassLoader(): ClassLoader = getClassLoader(T::class.java)

/**
 * 기본 ClassLoader를 반환합니다. context ClassLoader → system ClassLoader 순으로 fallback합니다.
 *
 * ```kotlin
 * val cl = getDefaultClassLoader()
 * val stream = cl.getResourceAsStream("META-INF/services/io.bluetape4k.SomeService")
 * ```
 */
fun getDefaultClassLoader(): ClassLoader = getContextClassLoader()

/**
 * 현재 스레드의 context ClassLoader를 반환합니다.
 * 실패 시 system ClassLoader로 fallback합니다.
 */
fun getContextClassLoader(): ClassLoader = resolveClassLoader { Thread.currentThread().contextClassLoader }

/**
 * system ClassLoader를 반환합니다.
 */
fun getSystemClassLoader(): ClassLoader = resolveClassLoader { ClassLoader.getSystemClassLoader() }

private inline fun resolveClassLoader(crossinline loader: () -> ClassLoader?): ClassLoader =
    runCatching { loader() }.getOrNull() ?: ClassLoader.getSystemClassLoader()
