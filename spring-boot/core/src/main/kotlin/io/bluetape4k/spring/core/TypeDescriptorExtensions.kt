package io.bluetape4k.spring.core

import org.springframework.core.convert.TypeDescriptor

/**
 * reified 타입의 원시 Java class로 [TypeDescriptor]를 생성합니다.
 *
 * Java [Class] 토큰을 사용하므로 `List<String>` 같은 매개변수 타입 인자는 보존하지 않습니다.
 */
inline fun <reified T : Any> typeDescriptorOf(): TypeDescriptor =
    TypeDescriptor.valueOf(T::class.java)
