package io.bluetape4k.hibernate.reactive.examples.model

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.support.hashOf
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Past
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.FetchProfile
import java.time.LocalDate


// NOTE: author 를 lazy 로 얻기 위해서는 @FetchProfile 을 이용해야 합니다.
@FetchProfile(
    name = "withAuthor",
    fetchOverrides = [
        FetchProfile.FetchOverride(
            entity = Book::class,
            association = "author",
            mode = FetchMode.JOIN
        ) // 현재는 FetchMode.JOIN 만 지원한다
    ]
)
@Entity
@Table(name = "books")
@Access(AccessType.FIELD)
class Book private constructor(
    val isbn: String,
    val title: String,
    @Past
    var published: LocalDate,
): AbstractValueObject() {

    companion object {
        @JvmStatic
        operator fun invoke(isbn: String = "", title: String = "", published: LocalDate = LocalDate.EPOCH): Book {
            return Book(isbn, title, published)
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L


    // NOTE: ManyToOne 의 FetchType을 LAZY 로 하면 Thread 범위를 벗어나 예외가 발생한다.
    // NOTE: 이럴 땐 LEFT JOIN FETCH 를 수행하던가 @FetchProfile 을 사용해야 한다
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var author: Author? = null


    override fun equalProperties(other: Any): Boolean =
        other is Book && isbn == other.isbn && title == other.title && published.isEqual(other.published)

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = if (id != 0L) id.hashCode() else hashOf(isbn, title, published)

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("id", id)
            .add("isbn", isbn)
            .add("title", title)
            .add("published", published)
    }
}
