package io.bluetape4k.hibernate.reactive.examples.stage

import io.bluetape4k.hibernate.reactive.examples.model.Author
import io.bluetape4k.hibernate.reactive.examples.model.Author_
import io.bluetape4k.hibernate.reactive.examples.model.Book
import io.bluetape4k.hibernate.reactive.examples.model.Book_
import io.bluetape4k.hibernate.reactive.stage.findAs
import io.bluetape4k.hibernate.reactive.stage.getAs
import io.bluetape4k.hibernate.reactive.stage.withSessionSuspending
import io.bluetape4k.hibernate.reactive.stage.withStatelessSessionSuspending
import io.bluetape4k.hibernate.reactive.stage.withTransactionSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import jakarta.persistence.criteria.CriteriaQuery
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.LocalDate
import java.time.Month

@Execution(ExecutionMode.SAME_THREAD)
class StageStatelessSessionExamples: AbstractStageTest() {

    companion object: KLogging()

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
    fun `load entity with stage session`() = runSuspendIO {
        sf.withSessionSuspending { session ->
            // NOTE: many-to-one 을 lazy로 fetch 하기 위해서 EntityGraph나 @FetchProfile 을 사용해야 합니다.
            //
            val book = session.enableFetchProfile("withAuthor").findAs<Book>(book1.id).await()
            val authors = session.findAs<Author>(author1.id, author2.id).await()

            log.debug { "book=${book}" }
            log.debug { "authors=${authors.joinToString()}" }
        }
    }

    @Test
    fun `find author and fetch books`() = runTest {
        sf.withStatelessSessionSuspending { session ->
            val author = session.getAs<Author>(author2.id).await()
            val books = session.fetch(author.books).await()
            log.debug { "${author.name} wrote ${books.size} books." }
            books.forEach { book ->
                log.debug { "title:${book.title}" }
            }
        }
    }


    @Test
    fun `find all book with fetch join`() = runSuspendIO {
        val sql = "SELECT b FROM Book b LEFT JOIN FETCH b.author a"
        val books = sf.withStatelessSessionSuspending { session ->
            session.createSelectionQuery(sql, Book::class.java).resultList.await()
        }
        books.forEach {
            println(it)
            println("\t${it.author}")
        }
        books shouldHaveSize 3
    }

    @Test
    fun `find all by entity graph`() = runSuspendIO {
        val criteria = sf.criteriaBuilder.createQuery(Book::class.java)
        val root = criteria.from(Book::class.java)
        criteria.select(root)

        val books = sf.withStatelessSessionSuspending { session ->
            val graph = session.createEntityGraph(Book::class.java)
            graph.addAttributeNodes(Book::author.name)

            val query = session.createQuery(criteria)
            query.setPlan(graph)

            query.resultList.await()
        }
        books.forEach {
            println(it)
        }
        books shouldHaveSize 3
    }

    @Test
    fun `find book by author name`() = runSuspendIO {
        val cb = sf.criteriaBuilder
        val criteria = cb.createQuery(Book::class.java)
        val book = criteria.from(Book::class.java)
        val author = book.join(Book_.author)

        criteria.where(cb.equal(author.get(Author_.name), author1.name))

        val books = sf.withStatelessSessionSuspending { session ->
            val graph = session.createEntityGraph(Book::class.java)
            graph.addAttributeNodes(Book_.author)

            session.createQuery(criteria).setPlan(graph).resultList.await()
        }
        books.forEach {
            println(it)
        }
        books shouldHaveSize 1
    }

    @Test
    fun `find all authors by book isbn`() = runSuspendIO {
        val cb = sf.criteriaBuilder
        val criteria = cb.createQuery(Author::class.java)
        val author = criteria.from(Author::class.java)
        val book = author.join(Author_.books)
        criteria.select(author)
            .where(cb.equal(book.get(Book_.isbn), book1.isbn))

        val authors = sf.withStatelessSessionSuspending { session ->
            session.createQuery(criteria).resultList.await()
        }
        // NOTE: author 만 로딩했으므로, books 에 접근하면 lazy initialization 예외가 발생합니다.
        authors.forEach {
            println(it)
            //            it.books.forEach { book ->
            //                println("book=$book")
            //            }
        }
    }

    @Test
    fun `find author and book by book isbn`() = runSuspendIO {
        val cb = sf.criteriaBuilder
        val criteria: CriteriaQuery<Author> = cb.createQuery(Author::class.java)
        val author = criteria.from(Author::class.java)
        val book = author.join(Author_.books)

        // where 조건
        criteria.select(author)
            .where(cb.equal(book.get(Book_.isbn), book2.isbn))

        val authors = sf.withStatelessSessionSuspending { session ->
            // inner join fetch
            val graph = session.createEntityGraph(Author::class.java)
            // graph.addSubgraph(Author_.books)
            graph.addAttributeNodes(Author_.books)

            session.createQuery(criteria)
                .setPlan(graph)
                .resultList
                .await()
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
