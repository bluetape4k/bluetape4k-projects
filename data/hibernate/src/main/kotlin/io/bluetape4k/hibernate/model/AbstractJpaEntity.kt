package io.bluetape4k.hibernate.model

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.logging.KLogging
import jakarta.persistence.Transient
import org.hibernate.Hibernate
import java.io.Serializable

/**
 * [JpaEntity]의 최상위 추상화 클래스입니다.
 */
abstract class AbstractJpaEntity<ID: Serializable>: AbstractPersistenceObject(), JpaEntity<ID> {

    companion object: KLogging() {
        /**
         * 엔티티가 저장되었다고 한다면, identifier 값이 할당되어 있을 것이고, identifier만으로 두 엔티티가 같은지 검사합니다.
         * @param id     현 엔티티의 identifer의 값
         * @param target 비교 대상 엔티티
         * @param <TId>  엔티티의 identifer의 수형
         * @return 두 엔티티의 identifer 가 같은지 여부
         */
        private fun <TId> hasSameNonDefaultId(id: TId, target: JpaEntity<*>): Boolean =
            id == target.id

        /**
         * transient object 는 아직 identifier 의 값이 없으므로, business logic 상 엔티티를 구분할 수 있는 값으로 비교해야 합니다.
         * 업무로직 상 구분 상 같은지 여부를 판단하는 메소드입니다.
         * @param target 비교할 대상 객체
         * @param <TId> 엔티티의 고유 값을 나타내는 identifier의 수형
         * @return 업무로직 상 두 값이 같은 값을 나타내는지 여부
         */
        private fun <TId> hasSameBusinessSignature(self: AbstractJpaEntity<*>, target: JpaEntity<*>): Boolean =
            self.equalProperties(target)
    }

    /**
     * 엔티티가 Persistent Object 인지 여부를 반환합니다.
     */
    @get:Transient
    override val isPersisted: Boolean get() = id != null

    /**
     * 엔티티가 같은 지 비교합니다.
     * 일반적인 Language 차원의 비교가 아닌,
     * 엔티티의 Identifier를 비교하던가, 저장 전 객체 (transient object) 인 경우에는 Business identifier 를 구해 비교합니다.
     *
     * 두 엔티티 모두 Persist 인 경우에만 Id 로 비교하고, 나머지 경우에는 business signature를 비교합니다.
     *
     * @param other 비교할 엔티티
     */
    override fun equals(other: Any?): Boolean {
        val target = other?.let { Hibernate.unproxy(it) } as? JpaEntity<*> ?: return false
        return when {
            isPersisted != target.isPersisted -> false
            isPersisted && target.isPersisted -> hasSameNonDefaultId(id, target)
            else                              -> hasSameBusinessSignature<ID>(this, target)
        }
    }

    /**
     * 엔티티의 고유 값을 제공합니다.
     * persistent object 인 경우에는 identifier의 hash code를 반환하고,
     * transient object 인 경우에는 business logic 에 의한 고유 값을 반환합니다.
     */
    override fun hashCode(): Int {
        return id?.hashCode() ?: System.identityHashCode(this)
    }

    /**
     * 로그 출력도 비용이 많이 드는 작업입니다. 꼭 필요한 정보만 출력하게 하면 성능 저하를 최소화할 수 있습니다.
     */
    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("id", id)
    }
}
