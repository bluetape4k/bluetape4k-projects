package io.bluetape4k.jwt.keychain.repository

import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.logging.KLogging

/**
 * [KeyChain] 회전 이력을 저장하고 조회하는 저장소 계약입니다.
 *
 * ## 동작/계약
 * - [current]는 현재 서명에 사용할 키체인을 반환합니다.
 * - [findOrNull]은 이전 키로 서명된 JWT 검증 시 `kid` 조회에 사용됩니다.
 * - [rotate]는 구현체 정책에 따라 조건부 회전을 수행하고, [forcedRotate]는 강제 교체를 수행합니다.
 * - 인스턴스가 백그라운드 작업을 소유하는 경우 [close]로 작업을 취소합니다.
 * - 저장소는 주입받은 외부 자원을 소유하지 않으며, [close]는 저장소가 소유한 자원만 해제합니다.
 *
 * ```kotlin
 * val current = repository.current()
 * val rotated = repository.rotate(KeyChain())
 * // current.id.isNotBlank() == true
 * // rotated == true || rotated == false
 * ```
 */
interface KeyChainRepository: AutoCloseable {

    companion object: KLogging() {
        /** 기본 보관 개수입니다. */
        const val DEFAULT_CAPACITY = 10

        /** 최소 보관 개수입니다. */
        const val MIN_CAPACITY = 2

        /** 최대 보관 개수입니다. */
        const val MAX_CAPACITY = 1000
    }

    /**
     * rotated key chain 의 최대 저장 갯수 (기본값은 DEFAULT_CAPACITY (10))
     */
    val capacity: Int

    /**
     * 현재 사용할 [KeyChain]을 가져옵니다.
     *
     * ## 동작/계약
     * - 구현체가 비어 있는 경우 내부 정책에 따라 새 키를 생성하거나 예외를 던질 수 있습니다.
     *
     * ```kotlin
     * val repository = InMemoryKeyChainRepository()
     * repository.forcedRotate(KeyChain())
     * val current = repository.current()
     * // current.id.isNotBlank() == true
     * ```
     */
    fun current(): KeyChain

    /**
     * [kid]에 해당하는 KeyChain 을 가져옵니다. rotated 된 key chain으로 만든 jwt를 파싱할 때 사용합니다.
     *
     * ## 동작/계약
     * - 해당 `kid`가 없으면 `null`을 반환합니다.
     *
     * ```kotlin
     * val repository = InMemoryKeyChainRepository()
     * val keyChain = KeyChain()
     * repository.forcedRotate(keyChain)
     * val found = repository.findOrNull(keyChain.id)
     * // found != null
     * val notFound = repository.findOrNull("unknown-kid")
     * // notFound == null
     * ```
     */
    fun findOrNull(kid: String): KeyChain?

    /**
     * 새로운 [keyChain] 을 current key chain으로 사용하기 rotate를 수행합니다.
     * 기존 keyChain 의 유효기간이 남았다면, rotate를 수행하지 않습니다.
     *
     * @param keyChain 새롭게 대체될 [KeyChain]
     * @return revoke 여부
     *
     * ```kotlin
     * val rotated = repository.rotate(KeyChain())
     * // rotated == true || rotated == false
     * ```
     */
    fun rotate(keyChain: KeyChain): Boolean

    /**
     * 새로운 [keyChain] 을 current key chain으로 사용하기 강제로 rotate를 수행합니다.
     *
     * @param keyChain 새롭게 대체될 [KeyChain]
     * @return revoke 여부
     *
     * ```kotlin
     * val rotated = repository.forcedRotate(KeyChain())
     * // rotated == true || rotated == false
     * ```
     */
    fun forcedRotate(keyChain: KeyChain): Boolean

    /**
     * 저장된 모든 KeyChain을 삭제합니다. NOTE: 테스트 시에만 사용하세요
     *
     * ```kotlin
     * val repository = InMemoryKeyChainRepository()
     * repository.forcedRotate(KeyChain())
     * repository.deleteAll()
     * // repository.findOrNull("any-kid") == null
     * ```
     */
    fun deleteAll()

    /**
     * 저장소가 소유한 백그라운드 작업을 종료합니다.
     *
     * 타이머가 없는 구현체는 기본 구현을 그대로 사용하면 됩니다. 구현체는 여러 번 호출해도
     * 안전하게 동작하도록 만들어야 합니다.
     */
    override fun close() = Unit
}
