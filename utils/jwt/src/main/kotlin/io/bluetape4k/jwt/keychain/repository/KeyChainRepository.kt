package io.bluetape4k.jwt.keychain.repository

import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.logging.KLogging

/**
 * [KeyChain] 정보를 저장소에 저장하여, 분산환경에서 공유하거나, 오래된 JWT를 파싱하는데 사용할 수 있다
 */
interface KeyChainRepository {

    companion object: KLogging() {
        const val DEFAULT_CAPACITY = 10
        const val MIN_CAPACITY = 2
        const val MAX_CAPACITY = 1000
    }

    /**
     * rotated key chain 의 최대 저장 갯수 (기본값은 DEFAULT_CAPACITY (10))
     */
    val capacity: Int

    /**
     * 현재 사용할 [KeyChain]을 가져옵니다.
     */
    fun current(): KeyChain

    /**
     * [kid]에 해당하는 KeyChain 을 가져옵니다. rotated 된 key chain으로 만든 jwt를 파싱할 때 사용합니다.
     */
    fun findOrNull(kid: String): KeyChain?

    /**
     * 새로운 [keyChain] 을 current key chain으로 사용하기 rotate를 수행합니다.
     * 기존 keyChain 의 유효기간이 남았다면, rotate를 수행하지 않습니다.
     *
     * @param keyChain 새롭게 대체될 [KeyChain]
     * @param keyChainTimeoutMillis key chain 의 유효 기간 (0 이면 무한대, 기본 값은 1일)
     * @return revoke 여부
     */
    fun rotate(keyChain: KeyChain): Boolean

    /**
     * 새로운 [keyChain] 을 current key chain으로 사용하기 강제로 rotate를 수행합니다.
     *
     * @param keyChain 새롭게 대체될 [KeyChain]
     * @param keyChainTimeoutMillis key chain 의 유효 기간 (0 이면 무한대, 기본 값은 1일)
     * @return revoke 여부
     */
    fun forcedRotate(keyChain: KeyChain): Boolean

    /**
     * 저장된 모든 KeyChain을 삭제합니다. NOTE: 테스트 시에만 사용하세요
     */
    fun deleteAll()
}
