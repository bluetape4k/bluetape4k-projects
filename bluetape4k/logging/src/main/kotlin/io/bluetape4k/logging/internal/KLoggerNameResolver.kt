package io.bluetape4k.logging.internal

import java.lang.reflect.Modifier

/**
 * 클래스 명으로부터 Logger name을 추출합니다.
 *
 * Kotlin 의 Companion, Inner Class 에 대해서는 제외하도록 합니다.
 */
internal object KLoggerNameResolver {

    internal fun name(action: () -> Unit): String {
        val name = action.javaClass.name

        return when {
            name.contains("Kt$") -> name.substringBefore("Kt$") // .trimEnd('.')
            name.contains("$")   -> name.substringBefore("$") // .trimEnd('.')
            else                 -> name
        }
    }

    internal fun <T: Any> name(forClass: Class<T>): String = unwrapCompanionClass(forClass).name

    private fun <T: Any> unwrapCompanionClass(clazz: Class<T>): Class<*> {
        if (clazz.enclosingClass != null) {
            runCatching {
                val field = clazz.enclosingClass.getField(clazz.simpleName)
                if (Modifier.isStatic(field.modifiers) && field.type == clazz) {
                    return clazz.enclosingClass
                }
            }
        }
        return clazz
    }
}
