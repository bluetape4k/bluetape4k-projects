package io.bluetape4k.hibernate.model

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.logging.KLogging
import jakarta.persistence.Transient
import org.hibernate.Hibernate
import java.io.Serializable

/**
 * JPA entity를 위한 base abstraction입니다.
 *
 * Equality는 Hibernate proxy를 해제한 effective entity type이 같은 경우에만
 * persisted identifier 또는 transient business signature를 사용합니다.
 * [hashCode]는 type 기반으로만 계산하여 transient entity가 영속화된 뒤에도
 * `HashSet`과 `HashMap`에서 같은 위치를 유지합니다.
 */
abstract class AbstractJpaEntity<ID: Serializable>: AbstractPersistenceObject(), JpaEntity<ID> {

    companion object: KLogging() {
        /**
         * 영속화된 [target]이 같은 non-null identifier를 가지는지 반환합니다.
         */
        private fun <TId> hasSameNonDefaultId(id: TId, target: JpaEntity<*>): Boolean =
            id == target.id

        /**
         * Hibernate proxy를 해제한 두 entity의 effective type이 같은지 반환합니다.
         */
        private fun hasSameEntityType(self: AbstractJpaEntity<*>, target: JpaEntity<*>): Boolean =
            Hibernate.getClass(self) == Hibernate.getClass(target)

        /**
         * 두 transient entity가 같은 business signature를 공유하는지 반환합니다.
         */
        private fun <TId> hasSameBusinessSignature(self: AbstractJpaEntity<*>, target: JpaEntity<*>): Boolean =
            self.equalProperties(target)
    }

    /**
     * 이 entity에 identifier가 이미 할당되어 있는지 반환합니다.
     */
    @get:Transient
    override val isPersisted: Boolean get() = id != null

    /**
     * Compares entities by identifier once both are persisted.
     *
     * Transient entities are compared by their business signature. A persisted
     * entity and a transient entity are never considered equal.
     *
     * @param other the candidate entity to compare
     */
    override fun equals(other: Any?): Boolean {
        val target = other?.let { Hibernate.unproxy(it) } as? JpaEntity<*> ?: return false
        return when {
            !hasSameEntityType(this, target) -> false
            isPersisted != target.isPersisted -> false
            isPersisted && target.isPersisted -> hasSameNonDefaultId(id, target)
            else                              -> hasSameBusinessSignature<ID>(this, target)
        }
    }

    /**
     * entity hash code를 반환합니다.
     *
     * Persisted 여부와 관계없이 Hibernate가 해석한 effective entity type만
     * 사용합니다. 따라서 identifier가 할당되어도 hash-based collection에서
     * entity의 위치가 바뀌지 않습니다. 서로 다른 type은 같은 hash를 가질 수
     * 있지만 equals가 false이므로 hash contract를 위반하지 않습니다.
     */
    override fun hashCode(): Int {
        return Hibernate.getClass(this).hashCode()
    }

    /**
     * Builds a concise string representation for logging.
     */
    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("id", id)
    }
}
