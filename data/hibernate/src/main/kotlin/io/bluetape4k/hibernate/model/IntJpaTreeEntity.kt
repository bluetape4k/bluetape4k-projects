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
 * Identifier가 Int 수형인 [JpaTreeEntity]의 추상 클래스입니다.
 *
 * NOTE: [AbstractJpaTreeEntity]는 QueryDSL 한계로 `@MappedSuperclass`를 붙이지 않으므로
 * Hibernate가 부모 클래스의 JPA 어노테이션을 인식하지 못합니다.
 * 따라서 [parent]와 [children] 필드의 JPA 어노테이션을 이 클래스에 재선언합니다.
 */
@MappedSuperclass
abstract class IntJpaTreeEntity<T>: AbstractJpaTreeEntity<T, Int>() where T: JpaTreeEntity<T> {

    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Int? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    override var parent: T? = null

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    override val children: MutableSet<T> = mutableSetOf()
}
