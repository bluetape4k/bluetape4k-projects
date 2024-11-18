package io.bluetape4k.hibernate.model

import jakarta.persistence.CascadeType
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.OneToMany

/**
 * Long 수형의 Identifier를 가지는 JPA Entity의 추상 클래스입니다.
 */
@MappedSuperclass
abstract class LongJpaTreeEntity<T>: AbstractJpaTreeEntity<T, Long>() where T: JpaTreeEntity<T> {

    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    override var parent: T? = null

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    override val children: MutableSet<T> = mutableSetOf()
}
