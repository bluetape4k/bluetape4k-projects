@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.getClassLoader
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberExtensionFunctions
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.kotlinFunction

/**
 * Kotlin 관련 유틸리티 클래스
 */
@Suppress("UNCHECKED_CAST")
object KotlinDetector: KLogging() {

    /**
     * Kotlin 언어로 정의된 수형에 대한 Kotlin Metadata Annotation 을 반환합니다.
     */
    val kotlinMetadata: Class<out Annotation>? by lazy {
        runCatching {
            Class.forName("kotlin.Metadata", false, getClassLoader<KotlinDetector>())
        }.getOrNull() as? Class<out Annotation>
    }

    /**
     * 현 프로세스에서 Kotlin을 사용할 수 있는지 알려준다
     */
    inline val isKotlinPresent: Boolean
        get() = kotlinMetadata != null

    /**
     * 지정한 수형이 Kotlin 으로 정의된 수형인가 판단합니다.
     *
     * @param clazz Class<*> 검사할 수형
     * @return Boolean 수형이 Kotlin으로 정의되었다면 true, 아니면 False를 반환
     */
    inline fun isKotlinType(clazz: Class<*>): Boolean {
        if (isKotlinPresent && clazz.getDeclaredAnnotation(kotlinMetadata) != null)
            return true

        return runCatching { clazz.kotlin.qualifiedName?.startsWith("kotlin.") == true }.getOrElse { false }
    }
}

/**
 * 현 수형이 Kotlin 으로 정의된 수형인가 판단합니다.
 *
 * ```
 * val isKotlinType = String::class.isKotlinType    // true
 * ```
 *
 * @receiver Class<*> 검사할 수형
 * @return Boolean 수형이 Kotlin으로 정의되었다면 true, 아니면 False를 반환
 */
inline val Class<*>.isKotlinType: Boolean
    get() = KotlinDetector.isKotlinType(this)

/**
 * 메소드가 `suspend` 메소드인지 판단합니다.
 *
 * ```
 * val isSuspendable = String::class.isSuspendableFunction("length")    // false
 * ```
 *
 * @receiver KClass<*> 클래스 정보
 * @param methodName 메소드 명
 * @return suspend 함수인지 여부
 */
inline fun KClass<*>.isSuspendFunction(methodName: String): Boolean {
    return memberFunctions.any { it.name == methodName && it.isSuspend } ||
            memberExtensionFunctions.any { it.name == methodName && it.isSuspend }
}

/**
 * 현 클래스의 `suspend` 메소드들의 컬렉션을 조회합니다.
 *
 * ```
 * val suspendFunctions = String::class.getSuspendableFunctions()
 * ```
 *
 * @receiver KClass<*> 클래스 정보
 * @return suspend 함수 컬렉션
 */
inline fun KClass<*>.getSuspendFunctions(): List<KFunction<*>> {
    return memberFunctions.filter { it.isSuspend } +
            memberExtensionFunctions.filter { it.isSuspend }
}

/**
 * [Method]가 `suspend` 메소드인지 판단합니다.
 *
 * ```
 * String::class.java.getMethod("length").isSuspendableFunction    // false
 * ```
 *
 * @receiver Method 메소드 정보
 * @return suspend 메소드이면 true, 아니면 false
 */
inline val java.lang.reflect.Method.isSuspend: Boolean
    get() = this.kotlinFunction?.isSuspend ?: false
