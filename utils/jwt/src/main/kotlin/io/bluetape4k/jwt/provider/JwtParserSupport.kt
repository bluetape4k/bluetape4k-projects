package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.JwtConsts.HEADER_ALGORITHM
import io.bluetape4k.jwt.JwtConsts.HEADER_KEY_ID
import io.bluetape4k.jwt.keychain.KeyChain
import io.jsonwebtoken.Header
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Locator
import java.security.Key
import java.util.concurrent.ConcurrentHashMap


internal val jwtParserCache = ConcurrentHashMap<JwtProvider, JwtParser>()

/**
 * [JwtProvider]에 대응하는 [JwtParser]를 캐싱하여 반환합니다.
 *
 * ## 동작/계약
 * - jjwt 0.13.0 내장 압축 알고리즘(DEF, GZIP)을 자동 지원합니다.
 * - 키 조회는 [Locator]를 통해 `kid` 헤더 기반으로 수행합니다.
 * - 검증 시 공개키(`publicKey`)를 사용합니다 (jjwt 0.12.x+ 요구사항).
 */
internal fun JwtProvider.currentJwtParser(): JwtParser =
    jwtParserCache.getOrPut(this) {
        Jwts.parser()
            .keyLocator(getKeyLocator { kid -> findKeyChain(kid) })
            .build()
    }

/**
 * JWT 헤더의 `kid`로 키체인을 조회하고 검증용 공개키를 반환하는 [Locator]를 생성합니다.
 *
 * ## 동작/계약
 * - `kid` 헤더가 없으면 [SecurityException]을 던집니다.
 * - 알고리즘 불일치 시 [SecurityException]을 던집니다.
 * - jjwt 0.12.x부터 검증에는 반드시 공개키를 사용해야 합니다.
 *
 * @param findKeyChain `kid`로 [KeyChain]을 조회하는 함수
 * @return 검증용 공개키를 반환하는 [Locator]
 */
internal fun getKeyLocator(findKeyChain: (String) -> KeyChain?): Locator<Key> {
    return Locator { header: Header ->
        val kid = header[HEADER_KEY_ID]?.toString()
        val keyChain = kid?.let { findKeyChain(it) }
            ?: throw SecurityException("Not found kid in jwt header.")

        val algorithm = header[HEADER_ALGORITHM] as? String
        if (algorithm != keyChain.algorithm.id) {
            throw SecurityException("Algorithm mismatch. jwt: $algorithm, keyChain: ${keyChain.algorithm.id}")
        }
        keyChain.keyPair.public
    }
}
