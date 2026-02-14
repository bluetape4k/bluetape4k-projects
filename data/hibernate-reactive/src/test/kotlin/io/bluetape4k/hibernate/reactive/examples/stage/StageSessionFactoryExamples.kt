package io.bluetape4k.hibernate.reactive.examples.stage

import io.bluetape4k.hibernate.criteria.createQueryAs
import io.bluetape4k.hibernate.criteria.from
import io.bluetape4k.hibernate.reactive.examples.model.Author
import io.bluetape4k.hibernate.reactive.examples.model.Author_
import io.bluetape4k.hibernate.reactive.examples.model.Book
import io.bluetape4k.hibernate.reactive.examples.model.Book_
import io.bluetape4k.hibernate.reactive.stage.createEntityGraphAs
import io.bluetape4k.hibernate.reactive.stage.createSelectionQueryAs
import io.bluetape4k.hibernate.reactive.stage.findAs
import io.bluetape4k.hibernate.reactive.stage.withSessionSuspending
import io.bluetape4k.hibernate.reactive.stage.withTransactionSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import jakarta.persistence.criteria.CriteriaQuery
import kotlinx.coroutines.future.await
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.hibernate.LazyInitializationException
import org.hibernate.graph.RootGraph
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertFailsWith

@Execution(ExecutionMode.SAME_THREAD)
class StageSessionFactoryExamples: AbstractStageTest() {

    companion object: KLoggingChannel()

    private val author1 = Author(faker.name().name())
    private val author2 = Author(faker.name().name())
    private val book1 = Book(
        faker.numerify("#-#####-###-#"),
        faker.book().title(),
        LocalDate.of(1994, Month.JANUARY, 1)
    )
    private val book2 = Book(
        faker.numerify("#-#####-###-#"),
        faker.book().title(),
        LocalDate.of(1999, Month.MAY, 1)
    )
    private val book3 = Book(
        faker.numerify("#-#####-###-#"),
        faker.book().title(),
        LocalDate.of(1992, Month.JUNE, 1)
    )

    @BeforeAll
    fun beforeAll() {
        author1.addBook(book1)
        author2.addBook(book2)
        author2.addBook(book3)

        runSuspendIO {
            sf.withTransactionSuspending { session ->
                session.persist(author1, author2).await()
            }
        }
    }

    @Test
    fun `stage session example`() = runSuspendIO {
        sf.withSessionSuspending { session -> // NOTE: many-to-one 을 lazy로 fetch 하기 위해서 EntityGraph나 @FetchProfile 을 사용해야 합니다.
            val book = session.enableFetchProfile("withAuthor").findAs<Book>(book2.id).await()

            book.shouldNotBeNull()
            book.author.shouldNotBeNull()
        }

        val authors = sf.withSessionSuspending { session ->
            session.findAs<Author>(author1.id, author2.id).await()
        }
        authors shouldBeEqualTo listOf(author1, author2)
    }

    @Test
    fun `find author and fetch books`() = runSuspendIO {
        sf.withSessionSuspending { session ->
            val author = session.findAs<Author>(author2.id).await()
            val books = session.fetch(author.books).await()
            log.debug { "${author.name} wrote ${books.size} books." }
            books.forEach { book ->
                log.debug { "book title:${book.title}" }
            }
        }
    }

    @Test
    fun `withTransactionSuspending 은 transaction 인자를 전달한다`() = runSuspendIO {
        val count = sf.withTransactionSuspending { session, transaction ->
            transaction.shouldNotBeNull()
            session.createSelectionQueryAs<Long>("select count(a) from Author a")
                .singleResult
                .await()
                .toLong()
        }

        count shouldBeEqualTo 2L
    }

    @Test
    fun `find all book with fetch join`() = runSuspendIO {
        val sql = "SELECT b FROM Book b LEFT JOIN FETCH b.author a"
        val books = sf.withSessionSuspending { session ->
            session.createSelectionQueryAs<Book>(sql).resultList.await()
        }
        books.forEach {
            println("book=$it, author=${it.author}")
        }
        books shouldHaveSize 3
    }


    @Test
    fun `find all by entity graph`() = runSuspendIO {
        val criteria = sf.criteriaBuilder.createQueryAs<Book>()
        val root = criteria.from<Book>()
        criteria.select(root)

        val books = sf.withSessionSuspending { session ->
            val graph = session.createEntityGraphAs<Book>()
            graph.addAttributeNodes(Book::author.name)

            val query = session.createQuery(criteria)
            query.setPlan(graph)

            query.resultList.await()
        }
        books.forEach {
            println("book=$it, author=${it.author}")
        }
        books shouldHaveSize 3
    }

    @Test
    fun `find book by author name`() = runSuspendIO {
        val cb = sf.criteriaBuilder
        val criteria = cb.createQueryAs<Book>()
        val book = criteria.from<Book>()
        val author = book.join(Book_.author)

        criteria.select(book).where(cb.equal(author.get(Author_.name), author1.name))

        val books = sf.withSessionSuspending { session ->
            val graph = session.createEntityGraphAs<Book>()
            graph.addAttributeNodes(Book_.author)

            val query = session.createQuery(criteria)
            query.setPlan(graph)
            query.resultList.await()
        }
        books.forEach {
            println("book=$it, author=${it.author}")
        }
        books shouldHaveSize 1
    }

    @Test
    fun `find all authors by book isbn`() = runSuspendIO {
        val cb = sf.criteriaBuilder
        val criteria = cb.createQueryAs<Author>()
        val author = criteria.from<Author>()
        val book = author.join(Author_.books)
        criteria.select(author).where(cb.equal(book.get(Book_.isbn), book1.isbn))

        val authors = sf.withSessionSuspending { session ->
            session.createQuery(criteria).resultList.await()
        }

        // NOTE: author 만 로딩했으므로, books 에 접근하면 lazy initialization 예외가 발생합니다.
        assertFailsWith<LazyInitializationException> {
            authors.forEach {
                it.books.forEach { book ->
                    println("book=$book")
                }
            }
        }
    }


    @Test
    fun `find author and book by book isbn`() = runSuspendIO {
        val cb = sf.criteriaBuilder
        val criteria: CriteriaQuery<Author> = cb.createQueryAs<Author>()
        val author = criteria.from<Author>()
        val book = author.join(Author_.books)

        // where 조건
        criteria.select(author).where(cb.equal(book.get(Book_.isbn), book2.isbn))

        val authors = sf.withSessionSuspending { session -> // inner join fetch
            val graph = session.createEntityGraphAs<Author>().apply {
                addAttributeNodes(Author_.books)
            } as RootGraph<Author>

            session.createQuery(criteria).setPlan(graph).resultList.await()
        }

        authors.forEach { a ->
            println(a)
            a.books.forEach { b ->
                println("\t$b")
            }
        }
        authors shouldHaveSize 1
        authors.forEach {
            it.books shouldHaveSize 2
        }
    }

    @Test
    fun `find author by book isbn and fetch book`() = runSuspendIO {
        val cb = sf.criteriaBuilder
        val criteria: CriteriaQuery<Author> = cb.createQueryAs<Author>()
        val author = criteria.from<Author>()
        val book = author.join(Author_.books)

        // where 조건
        criteria.select(author).where(cb.equal(book.get(Book_.isbn), book2.isbn))

        val authors = sf.withSessionSuspending { session ->
            session.createQuery(criteria).resultList.await().apply {
                forEach { author ->
                    session.fetch(author.books).await()
                }
            }
        }
        authors.forEach { a ->
            println(a)
            a.books.forEach { b ->
                println("\t$b")
            }
        }
        authors shouldHaveSize 1
        authors.forEach {
            it.books shouldHaveSize 2
        }
    }
}
