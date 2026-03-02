package io.bluetape4k.support

import java.lang.reflect.Method
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass

/**
 * non-null 프로퍼티를 "아직 초기화되지 않은 상태"로 선언하기 위한 더미 값을 반환합니다.
 *
 * ## 동작/계약
 * - 실제 구현은 `null as T` 캐스팅이므로 **안전하지 않으며**, 접근 시 NPE 등 런타임 오류를 유발할 수 있습니다.
 * - DI 프레임워크(Spring `@Autowired`, JSR-330 `@Inject`)의 필드/프로퍼티 주입을 위해 "선언만" 필요할 때 사용합니다.
 * - 가능하면 `lateinit var` 또는 `lazy` 위임 사용을 권장합니다.
 *
 * ```
 * @Inject
 * val repo: Repository = uninitialized()
 *
 * @Autowired
 * private val component: Component = uninitialized()
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <T> uninitialized(): T = null as T

/**
 * nullable 값을 Java의 [Optional]로 변환합니다.
 *
 * ## 동작/계약
 * - `null`이면 `Optional.empty()`를 반환합니다.
 * - `null`이 아니면 `Optional.of(value)`와 동일합니다.
 *
 * ```
 * val a: String? = "a"
 * val optA: Optional<String> = a.toOptional() // Optional.of("a")
 *
 * val b: String? = null
 * val optB: Optional<String> = b.toOptional() // Optional.empty()
 * ```
 */
fun <T: Any> T?.toOptional(): Optional<T> = Optional.ofNullable(this)

/**
 * 값이 [Optional]이면 내부 값을 꺼내고, 아니면 자기 자신을 반환합니다.
 *
 * ## 동작/계약
 * - `Optional.empty()`이면 `null`을 반환합니다.
 * - 2중 Optional(`Optional<Optional<*>>`)은 허용하지 않으며, 감지되면 예외를 발생시킵니다.
 *
 * @return Optional이면 내부 값 또는 `null`, 아니면 `this`
 *
 * ```kotlin
 * val present: Any = Optional.of("a")
 * println(present.unwrapOptional()) // "a"
 *
 * val empty: Any = Optional.empty<String>()
 * println(empty.unwrapOptional()) // null
 * ```
 */
fun Any.unwrapOptional(): Any? {
    if (this is Optional<*>) {
        if (!this.isPresent) {
            return null
        }
        val result = this.get()
        check(result !is Optional<*>) { "Multi-level Optional usage not allowed." }
        return result
    }
    return this
}

/**
 * 현재 객체가 Java 배열 타입인지 확인합니다.
 *
 * ## 동작/계약
 * - `IntArray`, `Array<*>` 등 모든 JVM 배열에 대해 `true`를 반환합니다.
 * - 컬렉션(List/Set 등)은 배열이 아니므로 `false`입니다.
 *
 * ```kotlin
 * println(intArrayOf(1, 2, 3).isArray) // true
 * println(arrayOf("a").isArray)        // true
 * println(listOf(1, 2, 3).isArray)     // false
 * ```
 */
val Any.isArray: Boolean get() = this.javaClass.isArray

/**
 * 여러 값을 조합해 해시 값을 계산합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Objects.hash]를 사용합니다.
 * - 입력 값이 `null`이어도 안전합니다.
 *
 * ```kotlin
 * val h1 = hashOf("a", 1, null)
 * val h2 = hashOf("a", 1, null)
 * println(h1 == h2) // true
 * ```
 */
fun hashOf(vararg values: Any?): Int = Objects.hash(*values)

/**
 * 두 객체의 동등성을 비교합니다.
 *
 * ## 동작/계약
 * - Kotlin의 `==`가 이미 null-safe 동등성 비교를 제공하므로, 이 함수는 더 이상 권장되지 않습니다.
 *
 * @deprecated Kotlin에서는 `a == b`를 사용하세요.
 */
@Deprecated("use `a == b` instead", ReplaceWith("a == b"))
fun areEquals(a: Any?, b: Any?): Boolean =
    (a == null && b == null) || (a != null && a == b)

/**
 * 두 객체를 null-safe로 비교하되, 둘 다 배열인 경우 배열 내용까지 비교합니다.
 *
 * ## 동작/계약
 * - 둘 중 하나라도 `null`이면 `false`입니다(둘 다 `null`일 때도 `false`).
 * - 동일 참조(`a === b`)이면 즉시 `true`입니다.
 * - 둘 다 배열이면 [arrayEquals]로 내용 비교를 수행합니다.
 *
 * ```kotlin
 * println(areEqualsSafe(null, null)) // false
 * println(areEqualsSafe(1, 1))       // true
 * println(areEqualsSafe(1, 2))       // false
 *
 * val a = arrayOf(1, 2, 3)
 * val b = arrayOf(1, 2, 3)
 * println(areEqualsSafe(a, b))       // true
 * ```
 */
fun areEqualsSafe(a: Any?, b: Any?): Boolean {
    if (a === b)
        return true

    if (a == null || b == null)
        return false

    if (a == b)
        return true

    if (a.javaClass.isArray && b.javaClass.isArray) {
        return arrayEquals(a, b)
    }

    return false
}

/**
 * 두 값이 모두 배열 타입일 때, 배열의 **내용(content)** 이 같은지 비교합니다.
 *
 * ## 동작/계약
 * - Primitive array / object array / unsigned array를 모두 지원합니다.
 * - 배열 타입이 아니거나, 배열 타입이 서로 다르면 `false`입니다.
 *
 * ```kotlin
 * val a = intArrayOf(1, 2, 3)
 * val b = intArrayOf(1, 2, 3)
 * println(arrayEquals(a, b)) // true
 * ```
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun arrayEquals(a: Any, b: Any): Boolean {
    return when (a) {
        is Array<*> if b is Array<*>         -> a.contentDeepEquals(b)
        is BooleanArray if b is BooleanArray -> a.contentEquals(b)
        is ByteArray if b is ByteArray       -> a.contentEquals(b)
        is CharArray if b is CharArray       -> a.contentEquals(b)
        is DoubleArray if b is DoubleArray   -> a.contentEquals(b)
        is FloatArray if b is FloatArray     -> a.contentEquals(b)
        is IntArray if b is IntArray         -> a.contentEquals(b)
        is LongArray if b is LongArray       -> a.contentEquals(b)
        is ShortArray if b is ShortArray     -> a.contentEquals(b)
        is UByteArray if b is UByteArray     -> a.contentEquals(b)
        is UShortArray if b is UShortArray   -> a.contentEquals(b)
        is UIntArray if b is UIntArray       -> a.contentEquals(b)
        is ULongArray if b is ULongArray     -> a.contentEquals(b)
        else                                 -> false
    }
}
//
///**
// * 컬렉션의 모든 요소가 not null 인 경우에만 [block]을 수행합니다.
// */
//infix fun <T: Any, R: Any> Collection<T?>.whenAllNotNull(block: (Collection<T>) -> R) {
//    if (this.all { it != null }) {
//        block(this.filterNotNull())
//    }
//}
//
///**
// * 컬렉션의 요소중 하나라도 null이 아니라면 null 이 아닌 요소들로만 [block]을 수행합니다.
// */
//infix fun <T: Any, R: Any> Collection<T?>.whenAnyNotNull(block: (Collection<T>) -> R) {
//    if (this.any { it != null }) {
//        block(this.filterNotNull())
//    }
//}

/**
 * null-safe 해시 코드를 계산합니다. 배열인 경우 배열 내용 기반 해시를 사용합니다.
 *
 * ## 동작/계약
 * - `null`이면 `0`을 반환합니다.
 * - 배열이면 `contentHashCode`/`contentDeepHashCode` 계열을 사용합니다.
 * - 배열이 아니면 일반 `hashCode()`를 반환합니다.
 *
 * ```kotlin
 * println((null as Any?).hashCodeSafe()) // 0
 * println(intArrayOf(1, 2, 3).hashCodeSafe() == intArrayOf(1, 2, 3).hashCodeSafe()) // true
 * ```
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun Any?.hashCodeSafe(): Int {
    if (this == null) {
        return 0
    }
    return if (this.isArray) {
        when (this) {
            is Array<*>    -> this.contentDeepHashCode()
            is BooleanArray -> this.contentHashCode()
            is CharArray   -> this.contentHashCode()
            is ByteArray    -> this.contentHashCode()
            is ShortArray  -> this.contentHashCode()
            is IntArray    -> this.contentHashCode()
            is LongArray   -> this.contentHashCode()
            is DoubleArray  -> this.contentHashCode()
            is FloatArray   -> this.contentHashCode()
            is UByteArray  -> this.contentHashCode()
            is UShortArray -> this.contentHashCode()
            is UIntArray   -> this.contentHashCode()
            is ULongArray  -> this.contentHashCode()
            else            -> Objects.hash(this)
        }
    } else {
        this.hashCode()
    }
}

/**
 * 객체의 "정체성(identity)" 문자열을 생성합니다.
 *
 * ## 동작/계약
 * - `null`이면 빈 문자열을 반환합니다.
 * - 형식은 `클래스 FQCN@identityHex` 입니다.
 * - identity 해시는 [System.identityHashCode] 기반입니다.
 *
 * ```kotlin
 * val obj = Any()
 * println(obj.identityToString()) // e.g. "java.lang.Object@27c170f0"
 * val x: Any? = null
 * println(x.identityToString())   // ""
 * ```
 */
fun Any?.identityToString(): String = when (this) {
    null -> ""
    else -> javaClass.name + "@" + this.identityHexString()
}

/**
 * 객체의 identity hash([System.identityHashCode])를 16진수 문자열로 반환합니다.
 *
 * ## 동작/계약
 * - `toString()`/`hashCode()` 오버라이딩과 무관하게 JVM identity hash를 사용합니다.
 *
 * ```kotlin
 * val obj = Any()
 * println(obj.identityHexString()) // e.g. "27c170f0"
 * ```
 */
fun Any.identityHexString(): String = Integer.toHexString(System.identityHashCode(this))

/**
 * 값을 사람이 읽기 쉬운 문자열로 변환합니다.
 *
 * ## 동작/계약
 * - `null`은 문자열 `"null"`로 변환합니다.
 * - 배열은 `contentToString()`/`contentDeepToString()`을 사용해 내용을 출력합니다.
 * - Map/Iterable/Sequence/Pair/Triple/Throwable/Optional 등은 내부 요소를 재귀적으로 [toStr]로 변환합니다.
 * - 변환 중 예외가 발생하면 `"<error ...>"` 형태의 문자열을 반환합니다.
 *
 * ```kotlin
 * println((null as Any?).toStr())           // "null"
 * println(intArrayOf(1, 2, 3).toStr())      // "[1, 2, 3]"
 * println(mapOf("a" to 1).toStr())          // "{a=1}"
 * println(Optional.of("x").toStr())         // "Optional[x]"
 * ```
 */
fun Any?.toStr(): String =
    try {
        when (this) {
            null                                             -> "null"
            is BooleanArray                                  -> this.contentToString()
            is ByteArray                                     -> this.contentToString()
            is CharArray                                     -> this.contentToString()
            is ShortArray                                    -> this.contentToString()
            is IntArray                                      -> this.contentToString()
            is LongArray                                     -> this.contentToString()
            is FloatArray                                    -> this.contentToString()
            is DoubleArray                                   -> this.contentToString()
            is Array<*>                                      -> this.contentDeepToString()
            is Map<*, *>                                     -> this.entries.joinToString(
                ", ",
                "{",
                "}"
            ) { "${it.key.toStr()}=${it.value.toStr()}" }
            is Iterable<*>                                   -> this.joinToString(", ", "[", "]") { it.toStr() }
            is Sequence<*>                                   -> this.joinToString(", ", "[", "]") { it.toStr() }
            is Pair<*, *>                                    -> "(${first.toStr()}, ${second.toStr()})"
            is Triple<*, *, *>                               -> "(${first.toStr()}, ${second.toStr()}, ${third.toStr()})"
            is Enum<*>                                       -> name
            is Throwable                                     -> buildString {
                append(this@toStr::class.qualifiedName)
                message?.let { append(": ").append(it) }
            }
            Void.TYPE.kotlin                                 -> "void"
            kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED -> "SUSPEND_MARKER"
            is Continuation<*>                               -> "continuation {}"
            is KClass<*>                                     -> this.simpleName ?: "<null name class>"
            is Method                                        -> name + "(" + parameterTypes.joinToString { it.simpleName } + ")"
            is Function<*>                                   -> "lambda {}"
            is Optional<*>                                   ->
                if (this.isPresent) "Optional[${this.get().toStr()}]"
                else "Optional.empty"
            else                                             -> toString()
        }
    } catch (thr: Throwable) {
        "<error \"$thr\">"
    }

/**
 * Java 모니터 메서드 `notify()`를 호출합니다.
 *
 * ## 동작/계약
 * - 호출 스레드는 반드시 `synchronized(this)` 블록(또는 동일 모니터)을 보유해야 합니다.
 * - 그렇지 않으면 [IllegalMonitorStateException]이 발생합니다.
 *
 * ```kotlin
 * val lock = Any()
 * synchronized(lock) {
 *     lock.notify()
 * }
 * ```
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Any.notify() {
    (this as java.lang.Object).notify()
}

/**
 * Java 모니터 메서드 `notifyAll()`를 호출합니다.
 *
 * ## 동작/계약
 * - 호출 스레드는 반드시 `synchronized(this)` 블록(또는 동일 모니터)을 보유해야 합니다.
 * - 그렇지 않으면 [IllegalMonitorStateException]이 발생합니다.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Any.notifyAll() {
    (this as java.lang.Object).notifyAll()
}

/**
 * Java 모니터 메서드 `wait()`를 호출합니다.
 *
 * ## 동작/계약
 * - 호출 스레드는 반드시 `synchronized(this)` 블록(또는 동일 모니터)을 보유해야 합니다.
 * - 스레드는 모니터를 반납하고 대기하며, 깨어나면 다시 모니터를 획득합니다.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Any.wait() {
    (this as java.lang.Object).wait()
}

/**
 * Java 모니터 메서드 `wait(timeoutMillis)`를 호출합니다.
 *
 * ## 동작/계약
 * - `timeoutMillis` 이후 자동으로 깨어날 수 있습니다.
 * - 호출 스레드는 반드시 `synchronized(this)` 블록(또는 동일 모니터)을 보유해야 합니다.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Any.wait(timeoutMillis: Long) {
    (this as java.lang.Object).wait(timeoutMillis)
}

/**
 * Java 모니터 메서드 `wait(timeoutMillis, nanos)`를 호출합니다.
 *
 * ## 동작/계약
 * - 시간 해상도는 JVM/OS에 의존합니다.
 * - 호출 스레드는 반드시 `synchronized(this)` 블록(또는 동일 모니터)을 보유해야 합니다.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Any.wait(timeoutMillis: Long, nanos: Int) {
    (this as java.lang.Object).wait(timeoutMillis, nanos)
}
