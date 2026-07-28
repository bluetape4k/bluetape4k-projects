package io.bluetape4k.hibernate.model

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.logging.KLogging
import jakarta.persistence.Transient
import org.hibernate.Hibernate
import java.io.Serializable

/**
 * JPA entity를 위한 base abstraction입니다.
 *
 * Equality uses the assigned identifier only when both entities are persisted.
 * Transient entities fall back to [equalProperties], so their default
 * [hashCode] must stay stable for hash-based collections before persistence.
 */
abstract class AbstractJpaEntity<ID: Serializable>: AbstractPersistenceObject(), JpaEntity<ID> {

    companion object: KLogging() {
        /**
         * 영속화된 [target]이 같은 non-null identifier를 가지는지 반환합니다.
         */
        private fun <TId> hasSameNonDefaultId(id: TId, target: JpaEntity<*>): Boolean =
            id == target.id

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
            isPersisted != target.isPersisted -> false
            isPersisted && target.isPersisted -> hasSameNonDefaultId(id, target)
            else                              -> hasSameBusinessSignature<ID>(this, target)
        }
    }

    /**
     * entity hash code를 반환합니다.
     *
     * Persisted entities use the identifier hash. Transient entities use the
     * Hibernate-resolved entity class hash so equal transient instances land in
     * the same hash bucket even before an identifier is assigned.
     */
    override fun hashCode(): Int {
        return id?.hashCode() ?: Hibernate.getClass(this).hashCode()
    }

    /**
     * Builds a concise string representation for logging.
     */
    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("id", id)
    }
}
