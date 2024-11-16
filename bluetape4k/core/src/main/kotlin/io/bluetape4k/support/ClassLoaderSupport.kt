package io.bluetape4k.support

import java.net.URL
import kotlin.reflect.KClass

private val EMPTY_URL_ARRAY = arrayOf<URL>()

fun getSystemURLs(): Array<URL> = ClassLoader.getSystemClassLoader().urls

fun getThreadURLs(): Array<URL> = Thread.currentThread().contextClassLoader.urls

val ClassLoader.urls: Array<URL>
    get() = when (this) {
        is java.net.URLClassLoader -> this@urls.getURLs()
        else                       -> EMPTY_URL_ARRAY
    }

fun ClassLoader.toStr(): String = when (this) {
    is java.net.URLClassLoader -> this@toStr.urls.contentToString()
    else                       -> this.toString()
}

fun getClassLoader(clazz: Class<*>): ClassLoader {
    //    return System.getSecurityManager()?.run {
    //        AccessController.doPrivileged(PrivilegedAction { clazz.classLoader })
    //    } ?: clazz.classLoader
    return clazz.classLoader
}

fun getClassLoader(kclass: KClass<*>): ClassLoader = getClassLoader(kclass.java)

inline fun <reified T> getClassLoader(): ClassLoader = getClassLoader(T::class.java)

fun getDefaultClassLoader(): ClassLoader = getContextClassLoader()

fun getContextClassLoader(): ClassLoader = getClassLoader { Thread.currentThread().contextClassLoader }

fun getSystemClassLoader(): ClassLoader = getClassLoader { ClassLoader.getSystemClassLoader() }

private inline fun getClassLoader(crossinline loader: () -> ClassLoader): ClassLoader {
    return runCatching { loader() }.getOrElse { Thread.currentThread().contextClassLoader }
}
