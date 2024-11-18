package io.bluetape4k.hibernate.reactive.examples.model

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.support.requireNotBlank
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "authors")
@Access(AccessType.FIELD)
class Author private constructor(
    @Column(nullable = false)
    val name: String,
): AbstractValueObject() {

    companion object {
        @JvmStatic
        operator fun invoke(name: String = "Unknown"): Author {
            name.requireNotBlank("name")
            return Author(name)
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L

    @OneToMany(mappedBy = "author", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var books: MutableList<Book> = mutableListOf()

    fun addBook(book: Book) {
        if (books.add(book)) {
            book.author = this
        }
    }

    fun removeBook(book: Book) {
        if (books.remove(book)) {
            book.author = null
        }
    }

    override fun equalProperties(other: Any): Boolean =
        other is Author && name == other.name

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = if (id != 0L) id.hashCode() else name.hashCode()

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}
