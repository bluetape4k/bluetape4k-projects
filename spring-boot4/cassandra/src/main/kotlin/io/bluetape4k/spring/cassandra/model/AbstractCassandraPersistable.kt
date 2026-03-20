package io.bluetape4k.spring.cassandra.model

import org.springframework.data.domain.Persistable
import org.springframework.data.util.ProxyUtils
import java.io.Serializable

/**
 * Cassandra 엔티티의 식별자 기반 동등성 비교를 제공하는 [Persistable] 추상 클래스입니다.
 *
 * ## 동작/계약
 * - [isNew]는 `id == null`일 때 `true`를 반환해 신규 엔티티 여부를 판단합니다.
 * - [equals]는 프록시를 실제 사용자 클래스([ProxyUtils.getUserClass])로 정규화한 뒤 `id`가 모두 존재하고 동일할 때만 `true`를 반환합니다.
 * - [hashCode]는 `id`가 있으면 `id.hashCode()`를, 없으면 객체 식별 해시를 사용합니다.
 *
 * ```kotlin
 * class UserEntity(private var pk: String? = null): AbstractCassandraPersistable<String>() {
 *     override fun getId(): String? = pk
 *     override fun setId(id: String) { pk = id }
 * }
 * // result == UserEntity().isNew()
 * ```
 *
 * @param PK 엔티티 식별자 타입
 */
abstract class AbstractCassandraPersistable<PK: Any>: Persistable<PK>,
                                                      Serializable {
    /**
     * 엔티티 식별자를 갱신합니다.
     *
     * ## 동작/계약
     * - 구현체는 전달된 `id`를 내부 식별자 필드에 반영해야 합니다.
     * - 별도 검증 규칙이 필요하면 구현체에서 예외를 던져 계약을 강화할 수 있습니다.
     *
     * ```kotlin
     * val entity = UserEntity()
     * entity.setId("user-1")
     * // result == (entity.id == "user-1")
     * ```
     */
    abstract fun setId(id: PK)

    override fun isNew(): Boolean = id == null

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other === this) {
            return true
        }
        if (this.javaClass != ProxyUtils.getUserClass(other)) {
            return false
        }
        return other is AbstractCassandraPersistable<*> && id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)

    override fun toString(): String = "Entity of type ${javaClass.simpleName} with id: $id"
}
