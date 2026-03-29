package io.bluetape4k.examples.cache.lettuce.domain

import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy

/**
 * Hibernate 2nd Level Cache 예제 엔티티.
 *
 * `@Cacheable` + `@Cache(NONSTRICT_READ_WRITE)`으로 2nd Level Cache 활성화.
 * Lettuce Near Cache가 auto-configured된 경우, 이 엔티티는 자동으로 캐싱된다.
 */
@Entity
@Table(name = "products")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "product")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Column
    val description: String? = null,

    @Column(nullable = false)
    val price: Double = 0.0,
)
