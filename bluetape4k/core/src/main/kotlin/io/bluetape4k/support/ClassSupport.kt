package io.bluetape4k.support

import org.apache.commons.lang3.ClassUtils
import kotlin.collections.isNullOrEmpty
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

val KClass<*>.packageName: String get() = this.java.packageName

val KFunction<*>.qualifiedName: String get() = this.javaMethod?.declaringClass?.name + name

/**
 * 객체를 지정한 수형으로 casting 합니다.
 *
 * ```
 * val obj: Any = "Hello"
 * val str = obj.cast<String>() // "Hello"
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <T: Any> Any.cast(kclass: KClass<T>): T =
    if (kclass.java.isInstance(this)) this as T
    else throw ClassCastException("${this::class} couldn't be cast to $kclass")

/**
 * 객체를 지정한 수형으로 casting 합니다.
 *
 * ```
 * val obj: Any = "Hello"
 * val str: String = obj.cast() // "Hello"
 * ```
 */
inline fun <reified T: Any> Any.cast(): T = cast(T::class)

/**
 * 지정한 수형의 인스턴스를 새로 생성합니다. 실패 시에는 null을 반환합니다.
 *
 * ```
 * val instance = java.lang.String::class.newInstance() // ""
 * ```
 */
fun <T: Any> Class<T>.newInstanceOrNull(): T? =
    runCatching { getDeclaredConstructor().newInstance() }.getOrNull()

/**
 * 지정한 수형의 인스턴스를 새로 생성합니다. 실패 시에는 null을 반환합니다.
 *
 * ```
 * val instance = String::class.newInstance() // ""
 * ```
 */
fun <T: Any> KClass<T>.newInstanceOrNull(): T? = java.newInstanceOrNull()

/**
 * 지정한 수형의 인스턴스를 새로 생성합니다. 실패 시에는 null을 반환합니다.
 *
 * ```
 * val instance = newInstanceOrNull("java.lang.String") // ""
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <T: Any> newInstanceOrNull(qualifiedName: String, classLoader: ClassLoader? = getContextClassLoader()): T? {
    qualifiedName.assertNotBlank("qualifiedName")

    return runCatching {
        val clazz = (classLoader?.loadClass(qualifiedName) ?: Class.forName(qualifiedName)) as? Class<T>
        clazz?.run { newInstanceOrNull() }
    }.getOrNull()
}


/**
 * [qualifiedName]에 해당하는 클래스가 존재하는지 동적으로 확인합니다.
 *
 * ```
 * val isPresent = classIsPresent("java.lang.String") // true
 * classIsPresent("healingpaper.kommons.NotExists") // false
 * ```
 */
fun classIsPresent(
    qualifiedName: String,
    classLoader: ClassLoader? = Thread.currentThread().contextClassLoader,
): Boolean {
    return try {
        (classLoader?.loadClass(qualifiedName) ?: Class.forName(qualifiedName)) != null
    } catch (ignored: Throwable) {
        false
    }
}

/**
 * 지정한 수형의 모든 상위 수형을 찾습니다.
 *
 * ```
 * val superTypes = String::class.findAllSuperTypes() // [String, Comparable, Serializable, Object]
 * ```
 */
fun Class<*>.findAllSuperTypes(): List<Class<*>> {
    val result = LinkedHashSet<Class<*>>()
    findAllSuperTypes(mutableListOf(this to supertypes()), mutableSetOf(this), result)
    return result.toList()
}

private tailrec fun findAllSuperTypes(
    nodes: MutableList<Pair<Class<*>, MutableList<Class<*>>>>,
    path: MutableSet<Class<*>>,
    visited: MutableSet<Class<*>>,
) {
    if (nodes.isEmpty()) {
        return
    }

    val (current, children) = nodes[nodes.lastIndex]

    if (children.isEmpty()) {
        visited.add(current)
        path.remove(current)
        nodes.removeLast()
    } else {
        val next = children.removeLastOrNull()
        next?.let {
            if (path.add(it)) {
                nodes.add(it to it.supertypes())
            }
        }
    }
    findAllSuperTypes(nodes, path, visited)
}

/**
 * 지정한 수형의 모든 상위 수형을 찾습니다.
 *
 * ```
 * val superTypes = String::class.java.supertypes() // [String, Comparable, Serializable, Object]
 * ```
 */
@Suppress("UNNECESSARY_SAFE_CALL")
private fun Class<*>.supertypes(): MutableList<Class<*>> =
    when {
        superclass == null         -> interfaces?.toMutableList() ?: mutableListOf()
        interfaces.isNullOrEmpty() -> mutableListOf(superclass)
        else                       -> ArrayList<Class<*>>(interfaces.size + 1).also {
            interfaces.toCollection(it)
            it.add(superclass)
        }
    }

/**
 * 지정한 수형의 축약된 이름을 반환합니다.
 *
 * ```
 * val abbrName = String::class.abbrName(3) // "Str"
 * ```
 */
fun <T> Class<T>.abbrName(lengthHint: Int): String =
    ClassUtils.getAbbreviatedName(this, lengthHint)

/**
 * 지정한 수형이 구현한 모든 인터페이스를 찾습니다.
 *
 * ```
 * val interfaces = String::class.getAllInterfaces() // [Serializable, Comparable, CharSequence, Appendable, Object]
 * ```
 */
fun <T> Class<T>.getAllInterfaces(): List<Class<*>> = ClassUtils.getAllInterfaces(this)

/**
 * 지정한 수형의 모든 상위 수형을 찾습니다.
 *
 * ```
 * val superclasses = String::class.getAllSuperclasses() // [CharSequence, Object]
 * ```
 */
fun <T> Class<T>.getAllSuperclasses(): List<Class<*>> = ClassUtils.getAllSuperclasses(this)
