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
    // WHY: `id!!` 대신 checkNotNull()을 사용하는 이유는 두 가지다.
    //      1) JPA 엔티티는 persist() 이전에는 id가 null이며, 이때 identifier에 접근하는 것은
    //         프로그래밍 오류이므로 NullPointerException보다 명확한 IllegalStateException이 적합하다.
    //      2) 한국어 메시지로 영속화 시점을 명시하여 스택트레이스 없이도 원인을 즉시 파악할 수 있다.
    val identifier: ID
        get() = checkNotNull(id) { "엔티티의 id가 null입니다. 영속화된 후에 identifier에 접근하세요." }
}
