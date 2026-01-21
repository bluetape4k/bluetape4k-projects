package io.bluetape4k.spring.beans

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import kotlin.reflect.KClass

private val log by lazy { KotlinLogging.logger {} }

/**
 * 지정된 수형의 Bean을 찾습니다. 없으면 예외를 발생합니다.
 *
 * ```
 * val bean = beanFactory.get<BeanClass>()
 * ```
 *
 * @receiver BeanFactory Bean Container
 * @param T 원하는 Bean의 수형
 */
inline fun <reified T: Any> BeanFactory.get(): T = getBean<T>()

/**
 * 지정된 이름의 Bean을 찾습니다. 없으면 null을 반환합니다.
 *
 * ```
 * val bean = beanFactory["beanName"]
 * ```
 *
 * @receiver BeanFactory Bean Container
 * @param name Bean 이름
 */
@Suppress("UNCHECKED_CAST")
operator fun <T: Any> BeanFactory.get(name: String): T? = getBean(name) as? T

/**
 * 지정된 수형의 Bean을 찾습니다. 없으면 예외를 발생합니다.
 *
 * ```
 * val bean = beanFactory[BeanClass::class]
 * ```
 *
 * @receiver BeanFactory Bean Container
 * @param requiredType 원하는 Bean의 수형
 *
 */
operator fun <T: Any> BeanFactory.get(requiredType: KClass<T>): T = getBean(requiredType.java)

/**
 * 지정된 수형의 Bean을 찾습니다. 없으면 예외를 발생합니다.
 *
 * ```
 * val bean = beanFactory[BeanClass::class.java]
 * ```
 *
 * @receiver BeanFactory
 * @param requiredType 원하는 Bean의 수형
 */
operator fun <T: Any> BeanFactory.get(requiredType: Class<T>): T = getBean(requiredType)

/**
 * 지정된 이름과 수형의 Bean을 찾습니다. 없으면 예외를 발생합니다.
 *
 * ```
 * val bean = beanFactory["beanName", BeanClass::class]
 * ```
 *
 * @receiver BeanFactory
 * @param name Bean 이름
 * @param requiredType 원하는 Bean의 수형
 */
operator fun <T: Any> BeanFactory.get(name: String, requiredType: Class<T>): T = getBean(name, requiredType)

/**
 * 지정된 이름과 수형의 Bean을 찾습니다. 없으면 예외를 발생합니다.
 *
 * ```
 * val bean = beanFactory["beanName", "arg1", "arg2"]
 * ```
 *
 * @receiver BeanFactory
 * @param name Bean 이름
 * @param args Bean 생성자 인자
 */
operator fun BeanFactory.get(name: String, vararg args: Any?): Any? = when {
    args.isEmpty() -> get(name) as? Any
    else           -> getBean(name, *args)
}

/**
 * 지정된 수형의 Bean을 찾습니다. 없으면 null 을 반환합니다.
 *
 * ```
 * val bean = beanFactory.findBean(BeanClass::class)
 * ```
 */
fun <T: Any> BeanFactory.findBean(requiredType: KClass<T>): T? =
    findBean(requiredType.java)

/**
 * 지정된 수형의 Bean을 찾습니다. 없으면 null 을 반환합니다.
 *
 * ```
 * val bean = beanFactory.findBean(BeanClass::class.java)
 * ```
 */
fun <T: Any> BeanFactory.findBean(requiredType: Class<T>): T? {
    return try {
        get(requiredType)
    } catch (e: BeansException) {
        log.warn(e) { "Fail to find bean. requiredType=$requiredType, return null." }
        null
    }
}

/**
 * 지정된 이름과 수형의 Bean을 찾습니다. 없으면 null 을 반환합니다.
 *
 * ```
 * val bean = beanFactory.findBean("beanName", BeanClass::class)
 * ```
 */
fun <T: Any> BeanFactory.findBean(name: String, requiredType: Class<T>): T? {
    return try {
        get(name, requiredType)
    } catch (e: BeansException) {
        log.warn(e) { "Fail to find bean. name=$name, requiredType=$requiredType, return null." }
        null
    }
}

/**
 * 지정된 이름과 수형의 Bean을 찾습니다. 없으면 null 을 반환합니다.
 *
 * ```
 * val bean = beanFactory.findBean("beanName", "arg1", "arg2")
 * ```
 */
fun <T: Any> BeanFactory.findBean(name: String, vararg args: Any?): Any? {
    return try {
        get(name, *args)
    } catch (e: BeansException) {
        log.warn(e) { "Fail to find bean. name=$name, args=$args, return null." }
        null
    }
}
