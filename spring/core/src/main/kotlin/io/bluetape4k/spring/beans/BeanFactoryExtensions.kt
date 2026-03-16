package io.bluetape4k.spring.beans

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import kotlin.reflect.KClass

private val log by lazy { KotlinLogging.logger {} }

/**
 * 제네릭 타입 [T]에 해당하는 빈을 조회합니다.
 *
 * ## 동작/계약
 * - 빈이 없으면 Spring [BeansException]을 그대로 전파합니다.
 * - 구현은 [BeanFactory.getBean] 확장을 호출합니다.
 *
 * ```kotlin
 * val service = beanFactory.get<MyService>()
 * // service != null
 * ```
 */
inline fun <reified T : Any> BeanFactory.get(): T = getBean<T>()

/**
 * 이름으로 빈을 조회하고 타입 캐스팅에 실패하면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `getBean(name)`을 호출한 뒤 안전 캐스팅(`as?`)합니다.
 * - 빈이 없으면 Spring 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val service: MyService? = beanFactory["myService"]
 * // service == null || service is MyService
 * ```
 */
@Suppress("UNCHECKED_CAST")
operator fun <T : Any> BeanFactory.get(name: String): T? = getBean(name) as? T

/**
 * [KClass]로 지정한 타입의 빈을 조회합니다.
 *
 * ## 동작/계약
 * - [requiredType]의 Java 클래스 타입으로 조회합니다.
 * - 빈이 없으면 Spring [BeansException]을 전파합니다.
 *
 * ```kotlin
 * val service = beanFactory[MyService::class]
 * // service != null
 * ```
 */
operator fun <T : Any> BeanFactory.get(requiredType: KClass<T>): T = getBean(requiredType.java)

/**
 * [Class]로 지정한 타입의 빈을 조회합니다.
 *
 * ## 동작/계약
 * - 구현은 `getBean(requiredType)` 호출입니다.
 * - 빈이 없으면 Spring [BeansException]을 전파합니다.
 *
 * ```kotlin
 * val service = beanFactory[MyService::class.java]
 * // service != null
 * ```
 */
operator fun <T : Any> BeanFactory.get(requiredType: Class<T>): T = getBean(requiredType)

/**
 * 이름과 타입으로 빈을 조회합니다.
 *
 * ## 동작/계약
 * - 구현은 `getBean(name, requiredType)` 호출입니다.
 * - 이름/타입이 맞지 않으면 Spring [BeansException]을 전파합니다.
 *
 * ```kotlin
 * val service = beanFactory["myService", MyService::class.java]
 * // service != null
 * ```
 */
operator fun <T : Any> BeanFactory.get(
    name: String,
    requiredType: Class<T>,
): T = getBean(name, requiredType)

/**
 * 이름과 생성자 인자로 빈을 조회합니다.
 *
 * ## 동작/계약
 * - [args]가 비어 있으면 `get(name)` 경로를 사용합니다.
 * - [args]가 있으면 `getBean(name, *args)`를 호출합니다.
 *
 * ```kotlin
 * val bean = beanFactory["myBean", "arg1", 2]
 * // bean != null
 * ```
 */
operator fun BeanFactory.get(
    name: String,
    vararg args: Any?,
): Any? =
    when {
        args.isEmpty() -> get(name) as? Any
        else -> getBean(name, *args)
    }

/**
 * 타입으로 빈을 조회하고 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Class] 기반 [findBean]으로 위임합니다.
 * - 조회 실패 시 예외 대신 `null`을 반환합니다.
 *
 * ```kotlin
 * val service = beanFactory.findBean(MyService::class)
 * // service == null || service is MyService
 * ```
 */
fun <T : Any> BeanFactory.findBean(requiredType: KClass<T>): T? = findBean(requiredType.java)

/**
 * 타입으로 빈을 조회하고 실패하면 경고 로그 후 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - [get] 호출에서 [BeansException]이 발생하면 예외를 삼키고 `null`을 반환합니다.
 * - 실패 정보는 경고 로그로 기록합니다.
 *
 * ```kotlin
 * val service = beanFactory.findBean(MyService::class.java)
 * // service == null || service is MyService
 * ```
 */
fun <T : Any> BeanFactory.findBean(requiredType: Class<T>): T? =
    runCatching { get(requiredType) }
        .getOrElse { e ->
            log.warn(e) { "Fail to find bean. requiredType=$requiredType, return null." }
            null
        }

/**
 * 이름과 타입으로 빈을 조회하고 실패하면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - [get] 호출에서 [BeansException]이 발생하면 예외 대신 `null`을 반환합니다.
 * - 실패 정보는 경고 로그로 기록합니다.
 *
 * ```kotlin
 * val service = beanFactory.findBean("myService", MyService::class.java)
 * // service == null || service is MyService
 * ```
 */
fun <T : Any> BeanFactory.findBean(
    name: String,
    requiredType: Class<T>,
): T? =
    runCatching { get(name, requiredType) }
        .getOrElse { e ->
            log.warn(e) { "Fail to find bean. name=$name, requiredType=$requiredType, return null." }
            null
        }

/**
 * 이름과 생성자 인자로 빈을 조회하고 실패하면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - [get] 호출에서 [BeansException]이 발생하면 예외 대신 `null`을 반환합니다.
 * - 실패 정보는 경고 로그로 기록합니다.
 *
 * ```kotlin
 * val bean = beanFactory.findBean("myBean", "arg1")
 * // bean == null || bean != null
 * ```
 */
fun <T : Any> BeanFactory.findBean(
    name: String,
    vararg args: Any?,
): Any? =
    runCatching { get(name, *args) }
        .getOrElse { e ->
            log.warn(e) { "Fail to find bean. name=$name, args=$args, return null." }
            null
        }
