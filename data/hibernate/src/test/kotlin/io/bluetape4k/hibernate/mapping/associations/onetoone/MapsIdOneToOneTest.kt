package io.bluetape4k.hibernate.mapping.associations.onetoone

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.hibernate.model.AbstractJpaEntity
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.hashOf
import io.bluetape4k.support.requireNotBlank
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.validation.constraints.NotBlank
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull

class MapsIdOneToOneTest(
    @param:Autowired private val authorRepo: AuthorRepository,
): AbstractHibernateTest() {

    companion object: KLogging() {
        fun newAuthor(): Author {
            return Author(faker.name().name()).apply {
                biography.information = faker.name().fullName()
                picture.path = faker.internet().url()
            }
        }
    }

    @Test
    fun `author and biography`() {
        val author = newAuthor()

        authorRepo.saveAndFlush(author)
        flushAndClear()

        author.id.shouldNotBeNull()
        author.biography.id shouldBeEqualTo author.id
        author.picture.id shouldBeEqualTo author.id

        val biography = em.findAs<Biography>(author.id!!)
        biography.shouldNotBeNull()
        biography.author shouldBeEqualTo author

        val picture = em.findAs<Picture>(author.id!!)
        picture.shouldNotBeNull()
        picture.author shouldBeEqualTo author

        // one-to-one 의 fetch type이 EAGER 라서 모두 JOIN 으로 처리한다
        val author2 = authorRepo.findByIdOrNull(author.id!!)!!
        author2 shouldBeEqualTo author
        author2.biography shouldBeEqualTo biography
        author2.biography.information shouldBeEqualTo biography.information
        author2.picture shouldBeEqualTo picture
        author2.picture.path shouldBeEqualTo picture.path

        // cascade delete (author -> biography, picture)
        authorRepo.delete(author2)
        flushAndClear()

        authorRepo.existsById(author.id!!).shouldBeFalse()
        em.findAs<Biography>(author.biography.id!!).shouldBeNull()
        em.findAs<Picture>(author.picture.id!!).shouldBeNull()
    }
}

@Entity(name = "onetoone_author")
@Access(AccessType.FIELD)
class Author private constructor(
    @field:NotBlank
    val name: String,
): IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(name: String): Author {
            name.requireNotBlank("name")
            return Author(name)
        }
    }

    @OneToOne(mappedBy = "author", cascade = [CascadeType.ALL])
    val picture: Picture = Picture(author = this)

    @OneToOne(mappedBy = "author", cascade = [CascadeType.ALL])
    val biography: Biography = Biography(author = this)

    override fun equalProperties(other: Any): Boolean =
        other is Author && name == other.name

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}

@Entity(name = "onetoone_picture")
@Access(AccessType.FIELD)
class Picture: IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(author: Author): Picture {
            return Picture().apply {
                this.author = author
            }
        }
    }

    // 이렇게 Id 를 설정하지만, 실제 TABLE에는 id column 은 없다. primary key @MapsId에 해당하는 author_id 가 된다.
//    @Id
//    override var id: Int? = null

    @MapsId
    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn(name = "author_id")
    var author: Author? = null

    var path: String? = null

    override fun equalProperties(other: Any): Boolean =
        other is Picture && author == other.author

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: hashOf(id, author)
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("author", author)
    }
}


@Entity(name = "onetoone_biography")
@Access(AccessType.FIELD)
class Biography: AbstractJpaEntity<Int>() {

    companion object {
        @JvmStatic
        operator fun invoke(author: Author): Biography {
            return Biography().apply {
                this.author = author
            }
        }
    }

    // 이렇게 Id 를 설정하지만, 실제 TABLE에는 id column 은 없다. primary key @MapsId에 해당하는 author_id 가 된다.
    @Id
    override var id: Int? = null

    @MapsId
    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn(name = "author_id")
    var author: Author? = null

    var information: String? = null

    override fun equalProperties(other: Any): Boolean =
        other is Biography && author == other.author

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: hashOf(id, author)
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("author", author)
    }
}

interface AuthorRepository: JpaRepository<Author, Int>
