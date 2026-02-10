package io.bluetape4k.support

import java.lang.reflect.Method
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass

/**
 * 선언된 필드 중 non null 수형에 대해 초기화 값을 지정하고자 할 때 사용합니다.
 * 또한 `@Autowired`, `@Inject` val 수형에 사용하기 좋다.
 *
 * **주의**: 이 함수는 안전하지 않으며 NPE를 발생시킬 수 있습니다.
 * 가능하면 `lateinit var` 또는 `lazy` 위임을 사용하는 것이 좋습니다.
 *
 * ```
 * @Inject
 * val x: Repository = uninitialized()
 * ```
 *
 * ```
 * @Autowired
 * private val component: Component = uninitialized()
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <T> uninitialized(): T = null as T

/**
 * 인스턴스를 [Optional]로 변환합니다.
 */
fun <T: Any> T?.toOptional(): Optional<T> = Optional.ofNullable(this)

/**
 * 변수가 [Optional]인 경우 [Any]로 변환합니다.
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
 * 객체가 [Array] 수형인지 확인합니다.
 */
val Any.isArray: Boolean get() = this.javaClass.isArray

/**
 * 객체들을 조합하여 hash 값을 계산합니다.
 */
fun hashOf(vararg values: Any?): Int = Objects.hash(*values)

/**
 * 두 객체가 같은지 판단합니다. (둘 다 null이면 true를 반환합니다)
 * Kotlin에서는 `==` 연산자가 null 안전하게 객체의 동등성을 비교하므로, 이 함수는 사용하지 않는 것이 좋습니다.
 */
@Deprecated("use `a == b` instead", ReplaceWith("a == b"))
fun areEquals(a: Any?, b: Any?): Boolean =
    (a == null && b == null) || (a != null && a == b)

/**
 * 두 객체가 모두 null인 경우는 false를 반환하고, array 인 경우에는 array 요소까지 비교합니다.
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
 * 두 Object 가 같은 것인가 검사한다. Array인 경우도 검사할 수 있습니다.
 *
 * ```
 * val a = arrayOf(1, 2, 3)
 * val b = arrayOf(1, 2, 3)
 * arrayEquals(a, b) // true
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
 * 변수의 hash 값을 계산합니다. null인 경우 0을 반환합니다.
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
 * 객체의 식별자를 문자열로 변환합니다.
 *
 * ```
 * val obj = Any()
 * obj.identityToString() // "java.lang.Object@1a2b3c4d"
 *
 * val x = null
 * x.identityToString() // ""
 * ```
 */
fun Any?.identityToString(): String = when (this) {
    null -> ""
    else -> javaClass.name + "@" + this.identityHexString()
}

/**
 * 객체의 식별자를 16진수 문자열로 변환합니다.
 *
 * ```
 * val obj = Any()
 * obj.identityHexString() // "1a2b3c4d"
 *
 * val x = null
 * x.identityHexString() // ""
 * ```
 */
fun Any.identityHexString(): String = Integer.toHexString(System.identityHashCode(this))

/**
 * 객체를 문자열로 변환합니다. 배열인 경우는 `contentToString()`을 사용합니다.
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
 * Java Object의 `notify()`을 호출합니다.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Any.notify() {
    (this as java.lang.Object).notify()
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Any.notifyAll() {
    (this as java.lang.Object).notifyAll()
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Any.wait() {
    (this as java.lang.Object).wait()
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Any.wait(timeoutMillis: Long) {
    (this as java.lang.Object).wait(timeoutMillis)
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Any.wait(timeoutMillis: Long, nanos: Int) {
    (this as java.lang.Object).wait(timeoutMillis, nanos)
}
