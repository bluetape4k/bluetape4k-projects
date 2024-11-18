package io.bluetape4k.hibernate.model

/**
 * Hibernate Persistece Object 를 나타내는 interface 입니다.
 */
interface PersistenceObject {

    /**
     * 현 Entity 가 영속화 되었는지 여부를 나타냅니다.
     */
    val isPersisted: Boolean

}
