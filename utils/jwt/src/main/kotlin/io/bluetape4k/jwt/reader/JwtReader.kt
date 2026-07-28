package io.bluetape4k.jwt.reader

import io.bluetape4k.support.requireNotBlank
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

    /**
     * JWT header의 `kid` 값입니다.
     *
     * ```kotlin
     * val provider = JwtProviderFactory.fixed(kid = "my-key")
     * val jwt = provider.compose { expirationAfterMinutes = 60 }
     * val reader = provider.parse(jwt)
     * val kid = reader.kid
     * // kid == "my-key"
     * ```
     */
    val kid: String?
        get() = header<String>("kid")

    /**
     * JWT 만료 시각을 epoch milliseconds로 표현한 값입니다.
     *
     * ## 동작/계약
     * - `exp` 클레임이 없으면 `null`을 반환합니다.
     */
    val expiresAtMillis: Long?
        get() = expiration?.time

    /**
     * 만료 TTL(Time To Live)을 milliseconds로 표현한 값입니다.
     *
     * ## 동작/계약
     * - `exp` 클레임이 없으면 [Long.MAX_VALUE]를 반환합니다.
     * - 이미 만료된 토큰이면 `0`을 반환합니다.
     *
     * ```kotlin
     * val provider = JwtProviderFactory.default()
     * val jwt = provider.compose { expirationAfterMinutes = 60 }
     * val reader = provider.parse(jwt)
     * val ttl = reader.remainingTtlMillis
     * // ttl <= 60 * 60 * 1000
     * ```
     */
    val remainingTtlMillis: Long
        get() = expiresAtMillis?.let { (it - System.currentTimeMillis()).coerceAtLeast(0L) } ?: Long.MAX_VALUE

    /**
     * 만료 TTL(Time To Live)을 milliseconds로 표현한 값입니다.
     *
     * @see remainingTtlMillis
     */
    val expiredTtl: Long
        get() = remainingTtlMillis

    /**
     * JWT 정보 만료 여부 (see: [getExpiration] )
     *
     * ## 동작/계약
     * - `exp`가 없으면 `false`입니다.
     *
     * ```kotlin
     * val provider = JwtProviderFactory.default()
     * val jwt = provider.compose { expirationAfterMinutes = 60 }
     * val reader = provider.parse(jwt)
     * val expired = reader.isExpired
     * // expired == false
     * ```
     */
    val isExpired: Boolean
        get() = expiresAtMillis?.let { it <= System.currentTimeMillis() } ?: false

    /**
     * 헤더 값을 조회합니다.
     *
     * ## 동작/계약
     * - [key]가 공백이면 검증 예외가 발생합니다.
     *
     * ```kotlin
     * val provider = JwtProviderFactory.default()
     * val jwt = provider.compose {
     *     header("x-author", "debop")
     *     expirationAfterMinutes = 60
     * }
     * val reader = provider.parse(jwt)
     * val author = reader.header("x-author")
     * // author == "debop"
     * ```
     */
    @JvmName("getHeader")
    fun header(key: String): Any? {
        key.requireNotBlank("key")
        return jws.header[key]
    }

    /**
     * 헤더 값을 타입 안전하게 조회합니다.
     *
     * ## 동작/계약
     * - 타입이 맞지 않으면 `null`을 반환합니다.
     *
     * ```kotlin
     * val provider = JwtProviderFactory.default()
     * val jwt = provider.compose {
     *     header("x-author", "debop")
     *     expirationAfterMinutes = 60
     * }
     * val reader = provider.parse(jwt)
     * val author = reader.header<String>("x-author")
     * // author == "debop"
     * ```
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
     *
     * ```kotlin
     * val provider = JwtProviderFactory.default()
     * val jwt = provider.compose { claim("userId", "alice"); expirationAfterMinutes = 60 }
     * val reader = provider.parse(jwt)
     * val userId = reader.claim("userId")
     * // userId == "alice"
     * ```
     */
    @JvmName("getClaim")
    fun claim(name: String): Any? {
        name.requireNotBlank("name")
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
