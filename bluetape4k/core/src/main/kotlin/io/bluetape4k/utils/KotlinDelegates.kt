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
 *
 * 예:
 * ```kotlin
 * data class Person(val name: String, val age: Int)
 *
 * val ctor: Constructor<Person> = Person::class.java.primaryConstructor()
 * // ctor.parameterCount == 2
 * ```
 */
fun <T: Any> Class<T>.primaryConstructor(): Constructor<T> =
    KotlinDelegates.primaryConstructor(this)

/**
 * Class의 기본 생성자를 찾습니다. 없으면 null을 반환
 *
 * 예:
 * ```kotlin
 * data class Person(val name: String, val age: Int)
 *
 * val ctor: Constructor<Person>? = Person::class.java.findPrimaryConstructor()
 * // ctor != null  (primary constructor가 존재함)
 *
 * // secondary constructor만 있는 클래스는 null 반환
 * class NoDefault { constructor(x: String) }
 * val ctor2: Constructor<NoDefault>? = NoDefault::class.java.findPrimaryConstructor()
 * // ctor2 == null
 * ```
 */
fun <T: Any> Class<T>.findPrimaryConstructor(): Constructor<T>? =
    KotlinDelegates.findPrimaryConstructor(this)

/**
 * [args] 인자를 받는 생성자를 이용하여 객체를 생성합니다.
 *
 * 기본값이 있는 파라미터에는 `null`을 전달하면 기본값이 사용됩니다.
 *
 * 예:
 * ```kotlin
 * data class Point(val x: Int, val y: Int = 0)
 *
 * val ctor: Constructor<Point> = Point::class.java.findPrimaryConstructor()!!
 * val p1: Point? = ctor.instantiateClass(3, 5)
 * // p1?.x == 3, p1?.y == 5
 *
 * // y에 null 전달 → 기본값 0 사용
 * val p2: Point? = ctor.instantiateClass(3, null)
 * // p2?.x == 3, p2?.y == 0
 * ```
 */
fun <T: Any> Constructor<T>.instantiateClass(vararg args: Any?): T? =
    KotlinDelegates.instantiateClass(this, *args)

/**
 * Kotlin Reflection을 이용하여 타입의 생성자를 찾거나 인스턴스를 생성하는 유틸리티 클래스
 *
 * 예:
 * ```kotlin
 * data class User(val id: Long, val name: String, val role: String = "USER")
 *
 * // 1. 기본 생성자 조회
 * val ctor: Constructor<User> = KotlinDelegates.primaryConstructor(User::class.java)
 *
 * // 2. 모든 인자 지정하여 인스턴스 생성
 * val admin: User? = KotlinDelegates.instantiateClass(ctor, 1L, "Alice", "ADMIN")
 * // admin?.id == 1L, admin?.name == "Alice", admin?.role == "ADMIN"
 *
 * // 3. 기본값 파라미터에 null 전달 → 기본값 "USER" 사용
 * val user: User? = KotlinDelegates.instantiateClass(ctor, 2L, "Bob", null)
 * // user?.role == "USER"
 * ```
 */
object KotlinDelegates: KLogging() {

    /**
     * 지정한 수형의 기본 생성자를 가져옵니다. 없으면 예외 [NoSuchElementException]를 발생시킵니다.
     *
     * 예:
     * ```kotlin
     * data class Item(val code: String, val qty: Int)
     *
     * val ctor: Constructor<Item> = KotlinDelegates.primaryConstructor(Item::class.java)
     * // ctor.parameterCount == 2
     *
     * // primary constructor가 없는 클래스는 NoSuchElementException 발생
     * class Legacy { constructor() }
     * KotlinDelegates.primaryConstructor(Legacy::class.java) // throws NoSuchElementException
     * ```
     *
     * @param clazz 대상 클래스
     * @return Constructor<T>
     */
    fun <T: Any> primaryConstructor(clazz: Class<T>): Constructor<T> =
        findPrimaryConstructor(clazz)
            ?: throw NoSuchElementException("Fail to find constructor for ${clazz.name}")

    /**
     * 지정한 수형의 기본 생성자 정보를 찾습니다. 없으면 null 반환
     *
     * 예:
     * ```kotlin
     * data class Tag(val key: String, val value: String)
     *
     * val ctor: Constructor<Tag>? = KotlinDelegates.findPrimaryConstructor(Tag::class.java)
     * // ctor != null
     *
     * // secondary constructor만 있는 클래스는 null 반환
     * class Multi { constructor(a: String); constructor(a: String, b: String) }
     * val ctor2 = KotlinDelegates.findPrimaryConstructor(Multi::class.java)
     * // ctor2 == null
     * ```
     *
     * @param clazz 대상 클래스
     * @return 생성자 정보, 없으면 null
     */
    fun <T: Any> findPrimaryConstructor(clazz: Class<T>): Constructor<T>? {
        return try {
            val primaryCtor = clazz.kotlin.primaryConstructor ?: return null
            val javaConstructor = primaryCtor.javaConstructor
            if (javaConstructor == null) {
                log.error { "Fail to find Java constructor for Kotlin primary constructor: ${clazz.name}" }
            }
            javaConstructor
        } catch (e: UnsupportedOperationException) {
            log.error(e) { "Fail to find primary constructor of Kotlin class [${clazz.name}]" }
            null
        }
    }

    /**
     * 생성자 정보를 이용하여 `T` 수형의 인스턴스를 생성합니다.
     *
     * 기본값이 있는 파라미터에 `null`을 전달하면 해당 파라미터의 기본값이 사용됩니다.
     * 인스턴스 생성에 실패하면 null을 반환합니다.
     *
     * 예:
     * ```kotlin
     * data class Config(val host: String, val port: Int = 8080, val tls: Boolean = false)
     *
     * val ctor: Constructor<Config> = KotlinDelegates.primaryConstructor(Config::class.java)
     *
     * // 모든 인자 지정
     * val c1: Config? = KotlinDelegates.instantiateClass(ctor, "example.com", 443, true)
     * // c1?.host == "example.com", c1?.port == 443, c1?.tls == true
     *
     * // port, tls에 null 전달 → 기본값 8080, false 사용
     * val c2: Config? = KotlinDelegates.instantiateClass(ctor, "localhost", null, null)
     * // c2?.port == 8080, c2?.tls == false
     * ```
     *
     * @param constructor 생성자 정보
     * @param args 생성자의 인자들
     * @return 생성된 인스턴스 또는 null
     */
    fun <T: Any> instantiateClass(constructor: Constructor<T>, vararg args: Any?): T? {
        return try {
            val kotlinCtor = constructor.kotlinFunction ?: return constructor.newInstance(*args)
            val parameters = kotlinCtor.parameters
            require(args.size <= parameters.size) {
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
