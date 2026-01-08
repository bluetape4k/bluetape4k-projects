package io.bluetape4k.exposed.shared.mapping.compositeid

import io.bluetape4k.ToStringBuilder
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.*

object BookSchema {
    val allTables = arrayOf(Publishers, Authors, Books, Reviews, Offices)

    /**
     * CompositeIdTable with 2 key columns - int & uuid (both db-generated)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS publishers (
     *      pub_id SERIAL,
     *      isbn_code uuid,
     *      publisher_name VARCHAR(32) NOT NULL,
     *
     *      CONSTRAINT pk_publishers PRIMARY KEY (pub_id, isbn_code)
     * )
     * ```
     * @see [Publisher]
     */
    object Publishers: CompositeIdTable("publishers") {
        val pubId = integer("pub_id").autoIncrement().entityId()
        val isbn = uuid("isbn_code").autoGenerate().entityId()
        val name = varchar("publisher_name", 32)

        override val primaryKey = PrimaryKey(pubId, isbn)
    }

    /**
     * [Publishers] 테이블을 참조하는 Author 테이블
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS authors (
     *      id SERIAL PRIMARY KEY,
     *      publisher_id INT NOT NULL,
     *      publisher_isbn uuid NOT NULL,
     *      pen_name VARCHAR(32) NOT NULL,
     *
     *      CONSTRAINT fk_authors_publisher_id_publisher_isbn__pub_id_isbn_code FOREIGN KEY (publisher_id, publisher_isbn)
     *      REFERENCES publishers(pub_id, isbn_code) ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object Authors: IntIdTable("authors") {
        val publisherId = integer("publisher_id")
        val publisherIsbn = uuid("publisher_isbn")
        val penName = varchar("pen_name", 32)

        // FK constraint with multiple columns is created as a table-level constraint
        init {
            foreignKey(publisherId, publisherIsbn, target = Publishers.primaryKey)
        }
    }

    /**
     * CompositeIdTable with 1 key column - int (db-generated)
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS books (
     *      book_id SERIAL PRIMARY KEY,
     *      title VARCHAR(32) NOT NULL,
     *      author_id INT NULL,
     *
     *      CONSTRAINT fk_books_author_id__id FOREIGN KEY (author_id)
     *      REFERENCES authors(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    object Books: CompositeIdTable("books") {
        val bookId = integer("book_id").autoIncrement().entityId()
        val title = varchar("title", 32)
        val author = optReference("author_id", Authors)

        override val primaryKey = PrimaryKey(bookId)
    }

    /**
     * CompositeIdTable with 2 key columns - string & long (neither db-generated)
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS reviews (
     *      code VARCHAR(8),
     *      "rank" BIGINT,
     *      book_id INT NOT NULL,
     *
     *      CONSTRAINT pk_reviews PRIMARY KEY (code, "rank"),
     *
     *      CONSTRAINT fk_reviews_book_id__book_id FOREIGN KEY (book_id)
     *      REFERENCES books(book_id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    object Reviews: CompositeIdTable("reviews") {
        val content = varchar("code", 8).entityId()
        val rank = long("rank").entityId()
        val book = integer("book_id")

        override val primaryKey = PrimaryKey(content, rank)

        init {
            foreignKey(book, target = Books.primaryKey)
        }
    }

    /**
     * CompositeIdTable with 3 key columns - string, string, & int (none db-generated)
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS offices (
     *      zip_code VARCHAR(8),
     *      "name" VARCHAR(64),
     *      area_code INT,
     *      staff BIGINT NULL,
     *      publisher_id INT NULL,
     *      publisher_isbn uuid NULL,
     *
     *      CONSTRAINT pk_offices PRIMARY KEY (zip_code, "name", area_code),
     *
     *      CONSTRAINT fk_offices_publisher_id_publisher_isbn__pub_id_isbn_code FOREIGN KEY (publisher_id, publisher_isbn)
     *      REFERENCES publishers(pub_id, isbn_code) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    object Offices: CompositeIdTable("offices") {
        val zipCode = varchar("zip_code", 8).entityId()
        val name = varchar("name", 64).entityId()
        val areaCode = integer("area_code").entityId()
        val staff = long("staff").nullable()
        val publisherId = integer("publisher_id").nullable()
        val publisherIsbn = uuid("publisher_isbn").nullable()

        override val primaryKey = PrimaryKey(zipCode, name, areaCode)

        init {
            // Publishers 는 publisherId, publisherIsbn 두 컬럼으로 구성된 PK
            foreignKey(publisherId, publisherIsbn, target = Publishers.primaryKey)
        }
    }

    class Publisher(id: EntityID<CompositeID>): CompositeEntity(id) {
        companion object: CompositeEntityClass<Publisher>(Publishers) {
            fun new(isbn: UUID, init: Publisher.() -> Unit): Publisher {
                // pubId 는 autoIncrement 이므로, isbn 만으로 CompositeID 를 생성
                val compositeId = CompositeID {
                    it[Publishers.isbn] = isbn
                }
                return Publisher.new(compositeId) {
                    init()
                }
            }
        }

        var name: String by Publishers.name
        val authors: SizedIterable<Author> by Author referrersOn Authors                // one-to-many
        val office: Office? by Office optionalBackReferencedOn Offices                  // one-to-one
        val allOffices: SizedIterable<Office> by Office optionalReferrersOn Offices     // one-to-many

        override fun toString(): String = ToStringBuilder(this)
            .add("name", name)
            .toString()
    }

    class Author(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Author>(Authors)

        var publisher by Publisher referencedOn Authors     // many-to-one
        var penName by Authors.penName

        override fun toString(): String = ToStringBuilder(this)
            .add("pen name", penName)
            .add("publisher id", publisher.id)
            .toString()
    }

    class Book(id: EntityID<CompositeID>): CompositeEntity(id) {
        companion object: CompositeEntityClass<Book>(Books)

        var title by Books.title
        var author by Author optionalReferencedOn Books.author  // many-to-one
        val review by Review backReferencedOn Reviews            // many-to-one

        override fun toString(): String = ToStringBuilder(this)
            .add("title", title)
            .add("author id", author?.id)
            .add("review id", review.id)
            .toString()
    }

    class Review(id: EntityID<CompositeID>): CompositeEntity(id) {
        companion object: CompositeEntityClass<Review>(Reviews)

        var book by Book referencedOn Reviews       // many-to-one

        override fun toString(): String = ToStringBuilder(this)
            .add("book id", book.id)
            .toString()
    }

    class Office(id: EntityID<CompositeID>): CompositeEntity(id) {
        companion object: CompositeEntityClass<Office>(Offices)

        var staff by Offices.staff
        var publisher by Publisher optionalReferencedOn Offices     // many-to-one

        override fun toString(): String = ToStringBuilder(this)
            .add("staff", staff)
            .add("publisher id", publisher?.id)
            .toString()
    }
}
