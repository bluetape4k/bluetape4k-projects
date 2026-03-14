package io.bluetape4k.hibernate.model

import io.bluetape4k.AbstractValueObject
import jakarta.persistence.Transient

/**
 * Hibernate Persistence Object 의 최상위 추상화 클래스입니다.
 */
abstract class AbstractPersistenceObject :
    AbstractValueObject(),
    PersistenceObject {
    @get:Transient
    override val isPersisted: Boolean = false
}
