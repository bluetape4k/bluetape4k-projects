package io.bluetape4k.hibernate.model

import java.io.Serializable

/**
 * JPA 용 Entity를 나타내는 interface
 */
interface JpaEntity<ID: Serializable>: PersistenceObject {

    /**
     * Entity identifier
     */
    var id: ID?

    /**
     * Entity의 not null인 identifier를 반환합니다.
     *
     * @return Entity의 identifier
     * @throws IllegalStateException Entity의 id 속성이 null인 경우 (아직 영속화되지 않은 엔티티)
     */
    val identifier: ID
        get() = checkNotNull(id) { "엔티티의 id가 null입니다. 영속화된 후에 identifier에 접근하세요." }
}
