package io.bluetape4k.support

import io.bluetape4k.GetterSetterOperator

/**
 * 시스템 설정 정보에 Kotlin 스타일로 접근할 수 있도록 해줍니다.
 *
 * ```kotlin
 *
 * // getter
 * val userDir = sysProperty["user.dir"]
 *
 * // setter
 * sysProperty["testcontainers.redis.port"] = "80"
 * ```
 */
val sysProperty: GetterSetterOperator<String, String> by lazy {
    GetterSetterOperator(
        getter = { key -> System.getProperty(key) ?: EMPTY_STRING },
        setter = { key, value -> System.setProperty(key, value) }
    )
}
