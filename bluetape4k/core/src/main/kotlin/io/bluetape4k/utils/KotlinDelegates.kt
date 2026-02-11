package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import java.lang.reflect.Constructor
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.kotlinFunction

/**
 * Class의 기본 생성자를 반환합니다.
 */
fun <T: Any> Class<T>.primaryConstructor(): Constructor<T> =
    KotlinDelegates.primaryConstructor(this)

/**
 * Class의 기본 생성자를 찾습니다. 없으면 null을 반환
 */
fun <T: Any> Class<T>.findPrimaryConstructor(): Constructor<T>? =
    KotlinDelegates.findPrimaryConstructor(this)

/**
 * [args] 인자를 받는 생성자를 이용하여 객체를 생성합니다.
 */
fun <T: Any> Constructor<T>.instantiateClass(vararg args: Any?): T? =
    KotlinDelegates.instantiateClass(this, *args)

/**
 * Kotlin Reflection을 이용하여 타입의 생성자를 찾거나 인스턴스를 생성하는 유틸리티 클래스
 */
object KotlinDelegates: KLogging() {

    /**
     * 지정한 수형의 기본 생성자를 가져옵니다. 없으면 예외 [NoSuchElementException]를 발생시킵니다.
     * @param clazz 대상 클래스
     * @return Constructor<T>
     */
    fun <T: Any> primaryConstructor(clazz: Class<T>): Constructor<T> =
        findPrimaryConstructor(clazz)
            ?: throw NoSuchElementException("Fail to find constructor for ${clazz.name}")

    /**
     * 지정한 수형의 기본 생성자 정보를 찾습니다. 없으면 null 반환
     * @param clazz 대상 클래스
     * @return 생성자 정보, 없으면 null
     */
    fun <T: Any> findPrimaryConstructor(clazz: Class<T>): Constructor<T>? {
        return try {
            val primaryCtor = clazz.kotlin.primaryConstructor ?: return null
            primaryCtor.javaConstructor.also {
                if (it == null) {
                    log.error { "Fail to find Java constructor for Kotlin primary constructor: ${clazz.name}" }
                }
            }
        } catch (e: UnsupportedOperationException) {
            log.error(e) { "Fail to find primary constructor of Kotlin class [${clazz.name}]" }
            null
        }
    }

    /**
     * 생성자 정보를 이용하여 `T` 수형의 인스턴스를 생성합니다.
     * @param constructor 생성자 정보
     * @param args 생성자의 인자들
     * @return 생성된 인스턴스 또는 null
     */
    fun <T: Any> instantiateClass(constructor: Constructor<T>, vararg args: Any?): T? {
        return try {
            val kotlinCtor = constructor.kotlinFunction ?: return constructor.newInstance(*args)
            val parameters = kotlinCtor.parameters
            check(args.size <= parameters.size) {
                "Number of provided arguments should be less than or equal to number of constructor parameters."
            }

            val argParams = HashMap<KParameter, Any?>(parameters.size)
            args.forEachIndexed { i, arg ->
                val isOptional = parameters[i].isOptional && arg == null
                if (!isOptional) {
                    argParams[parameters[i]] = arg
                }
            }
            kotlinCtor.callBy(argParams)
        } catch (e: Exception) {
            log.error(e) { "Fail to instantiate class [${constructor.declaringClass.name}]" }
            null
        }
    }
}
