package io.bluetape4k.hibernate.model

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass

/**
 * Long 수형의 Identifier를 가지는 JPA Entity의 추상 클래스입니다.
 */
@MappedSuperclass
abstract class LongJpaEntity: AbstractJpaEntity<Long>() {

    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Long? = null

}
