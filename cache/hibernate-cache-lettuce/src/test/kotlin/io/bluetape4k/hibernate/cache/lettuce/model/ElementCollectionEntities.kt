package io.bluetape4k.hibernate.cache.lettuce.model

import io.bluetape4k.support.hashOf
import jakarta.persistence.Cacheable
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable

@Entity
@Table(name = "articles")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Article: Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var title: String = ""

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "article_tags", joinColumns = [JoinColumn(name = "article_id")])
    @Column(name = "tag")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    val tags: MutableSet<String> = linkedSetOf()

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "article_ratings", joinColumns = [JoinColumn(name = "article_id")])
    @Column(name = "rating")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    val ratings: MutableList<Int> = mutableListOf()

    override fun equals(other: Any?): Boolean =
        other is Article && id == other.id && title == other.title && tags == other.tags && ratings == other.ratings

    override fun hashCode(): Int =
        id?.hashCode() ?: hashOf(title, tags, ratings)

    override fun toString(): String =
        "Article(id=$id, title='$title', tags=$tags, ratings=$ratings)"
}
