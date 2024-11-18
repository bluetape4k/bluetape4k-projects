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
     * @throws IllegalStateException Entity의 id 속성이 null인 경우
     */
    val identifier: ID get() = id!!
}
