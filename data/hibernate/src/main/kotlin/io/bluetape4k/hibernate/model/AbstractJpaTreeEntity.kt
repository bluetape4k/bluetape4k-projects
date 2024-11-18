package io.bluetape4k.hibernate.model

import jakarta.persistence.CascadeType
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.io.Serializable


/**
 * Self reference 를 가지는 Tree 구조의 엔티티인 [JpaTreeEntity]의 추상 클래스입니다.
 *
 * @param T  entity type
 * @param ID identifier type
 *
 * NOTE: 이 클래스에는 `@MappedSuperclass` 어노테이션이 붙이지 마시고, `IntJpaTreeEntity`와 `LongJpaTreeEntity` 클래스에만 붙이세요.
 */
abstract class AbstractJpaTreeEntity<T, ID: Serializable>
    : AbstractJpaEntity<ID>(), JpaTreeEntity<T> where T: JpaTreeEntity<T> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    override var parent: T? = null

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    override val children: MutableSet<T> = mutableSetOf()

}
