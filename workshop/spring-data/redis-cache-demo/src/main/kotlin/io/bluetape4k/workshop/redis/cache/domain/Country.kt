package io.bluetape4k.workshop.redis.cache.domain

import io.bluetape4k.support.randomString
import java.io.Serializable

/**
 * Country 정보 - Redis에 캐시합니다.
 *
 * @property code Country code
 */
data class Country(
    val code: String,
): Serializable {

    /**
     * 캐시 크기를 키워서 Value Serializer (ZstdFury)를 테스트합니다.
     */
    val name: String = randomString(256)

    /**
     * 캐시 크기를 키워서 Value Serializer (ZstdFury)를 테스트합니다.
     */
    val bytes = randomString(1024).toByteArray()
}
