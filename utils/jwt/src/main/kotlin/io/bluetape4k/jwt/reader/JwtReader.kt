package io.bluetape4k.jwt.reader

import io.bluetape4k.support.assertNotBlank
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import java.io.Serializable

/**
 * [Jws] 의 정보를 제공해주는 Reader 입니다.
 *
 * ## 동작/계약
 * - 내부 [jws] payload를 `Claims`로 위임해 표준 클레임 접근자를 그대로 사용할 수 있습니다.
 * - [header], [claim]의 key/name은 공백이면 검증 예외가 발생합니다.
 * - [isExpired]는 `expiration` 클레임 기준으로 현재 시각과 비교합니다.
 *
 * ```kotlin
 * val reader = provider.parse(jwt)
 * val claim = reader.claim<String>("claim1")
 * // claim == "value"
 * ```
 */
class JwtReader(
    internal val jws: Jws<Claims>,
): Claims by jws.payload, Serializable {

    /** JWT header의 `kid` 값입니다. */
    val kid: String?
        get() = header<String>("kid")

    /**
     * Expiration TTL (Time To Live) (Milliseconds)
     *
     * ## 동작/계약
     * - `exp` 클레임이 없으면 [Long.MAX_VALUE]를 반환합니다.
     */
    val expiredTtl: Long
        get() = expiration?.time ?: Long.MAX_VALUE

    /**
     * JWT 정보 만료 여부 (see: [getExpiration] )
     *
     * ## 동작/계약
     * - `exp`가 없으면 `false`입니다.
     */
    val isExpired: Boolean
        get() = expiredTtl <= System.currentTimeMillis()

    /**
     * 헤더 값을 조회합니다.
     *
     * ## 동작/계약
     * - [key]가 공백이면 검증 예외가 발생합니다.
     *
     * ```kotlin
     * val author = reader.header<String>("x-author")
     * // author == "debop"
     * ```
     */
    @JvmName("getHeader")
    fun header(key: String): Any? {
        key.assertNotBlank("key")
        return jws.header[key]
    }

    /**
     * 헤더 값을 타입 안전하게 조회합니다.
     *
     * ## 동작/계약
     * - 타입이 맞지 않으면 `null`을 반환합니다.
     */
    @JvmName("getHeaderInline")
    inline fun <reified T: Any> header(key: String): T? {
        return header(key) as? T
    }

    /**
     * 클레임 값을 조회합니다.
     *
     * ## 동작/계약
     * - [name]이 공백이면 검증 예외가 발생합니다.
     */
    @JvmName("getClaim")
    fun claim(name: String): Any? {
        name.assertNotBlank("name")
        return jws.payload[name]
    }

    /**
     * 클레임 값을 타입 안전하게 조회합니다.
     *
     * ## 동작/계약
     * - 타입이 맞지 않으면 `null`을 반환합니다.
     *
     * ```kotlin
     * val claim3 = reader.claim<Long>("claim3")
     * // claim3 != null
     * ```
     */
    @JvmName("getClaimInline")
    inline fun <reified T: Any> claim(name: String): T? {
        return claim(name) as? T
    }
}
