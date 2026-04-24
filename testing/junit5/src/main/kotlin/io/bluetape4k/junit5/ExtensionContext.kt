package io.bluetape4k.junit5

import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.reflect.KClass

/**
 * 주어진 Java [Class]와 현재 컨텍스트로 고유 [ExtensionContext.Namespace]를 생성합니다.
 *
 * ## 동작/계약
 * - `Namespace.create(clazz, this)`를 호출해 클래스+컨텍스트 조합의 고유 네임스페이스를 반환합니다.
 * - 동일 클래스라도 컨텍스트가 다르면 별도 네임스페이스가 됩니다.
 *
 * ```kotlin
 * val ns = context.namespace(MyExtension::class.java)
 * // context.getStore(ns)로 확장별 저장소 획득
 * ```
 */
internal fun ExtensionContext.namespace(clazz: Class<*>): ExtensionContext.Namespace =
    ExtensionContext.Namespace.create(clazz, this)

/**
 * 주어진 Kotlin [KClass]와 현재 컨텍스트로 고유 [ExtensionContext.Namespace]를 생성합니다.
 *
 * ## 동작/계약
 * - Kotlin 클래스를 Java 클래스로 변환 후 [namespace] 오버로드에 위임합니다.
 *
 * ```kotlin
 * val ns = context.namespace(MyExtension::class)
 * ```
 */
internal fun ExtensionContext.namespace(kclazz: KClass<*>): ExtensionContext.Namespace =
    ExtensionContext.Namespace.create(kclazz.java, this)

/**
 * 주어진 Java [Class] 기반 네임스페이스의 [ExtensionContext.Store]를 반환합니다.
 *
 * ## 동작/계약
 * - [namespace]로 네임스페이스를 생성하고 [ExtensionContext.getStore]로 store를 획득합니다.
 *
 * ```kotlin
 * val store = context.store(StopwatchExtension::class.java)
 * store.put("key", value)
 * ```
 */
internal fun ExtensionContext.store(clazz: Class<*>): ExtensionContext.Store =
    getStore(namespace(clazz))

/**
 * 주어진 Kotlin [KClass] 기반 네임스페이스의 [ExtensionContext.Store]를 반환합니다.
 *
 * ## 동작/계약
 * - [namespace]로 네임스페이스를 생성하고 [ExtensionContext.getStore]로 store를 획득합니다.
 *
 * ```kotlin
 * val store = context.store(StopwatchExtension::class)
 * store.put("startNano", System.nanoTime())
 * ```
 */
internal fun ExtensionContext.store(kclazz: KClass<*>): ExtensionContext.Store =
    getStore(namespace(kclazz))
