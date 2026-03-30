package io.bluetape4k.hibernate.mapping.naturalid

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.support.uninitialized
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.hibernate.Session
import org.hibernate.annotations.NaturalId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository

class NaturalIdTest: AbstractHibernateTest() {

    @Autowired
    private val bookRepo: NaturalIdBookRepository = uninitialized()

    @Test
    fun `NaturalId 로 엔티티를 조회할 수 있다`() {
        val book = NaturalIdBook(
            isbn = "978-89-1234-567-8",
            title = "Hibernate NaturalId Guide",
        )
        bookRepo.save(book)
        flushAndClear()

        val session = em.unwrap(Session::class.java)
        val loaded = session.bySimpleNaturalId(NaturalIdBook::class.java).load(book.isbn)

        loaded.shouldNotBeNull()
        loaded.id shouldBeEqualTo book.id
        loaded.title shouldBeEqualTo "Hibernate NaturalId Guide"
    }
}

@Entity(name = "natural_id_book")
@Table(name = "natural_id_book")
class NaturalIdBook(
    @NaturalId
    @Column(nullable = false, unique = true, updatable = false)
    var isbn: String = "",

    @Column(nullable = false)
    var title: String = "",
): IntJpaEntity() {

    override fun equalProperties(other: Any): Boolean =
        other is NaturalIdBook &&
                isbn == other.isbn &&
                title == other.title
}

interface NaturalIdBookRepository: JpaRepository<NaturalIdBook, Int>
