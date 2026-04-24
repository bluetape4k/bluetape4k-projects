package io.bluetape4k.hibernate.reactive.examples.mutiny

import io.bluetape4k.hibernate.reactive.examples.model.Author
import io.bluetape4k.hibernate.reactive.examples.model.Book
import io.bluetape4k.hibernate.reactive.mutiny.createNativeQueryAs
import io.bluetape4k.hibernate.reactive.mutiny.createQueryAs
import io.bluetape4k.hibernate.reactive.mutiny.findAs
import io.bluetape4k.hibernate.reactive.mutiny.getAs
import io.bluetape4k.hibernate.reactive.mutiny.getReferenceAs
import io.bluetape4k.hibernate.reactive.mutiny.withSessionSuspending
import io.bluetape4k.hibernate.reactive.mutiny.withStatelessSessionSuspending
import io.bluetape4k.hibernate.reactive.mutiny.withTransactionSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.hibernate.LockMode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.LocalDate
import java.time.Month

/**
 * Mutiny API 미커버 함수 추가 테스트
 *
 * - [findAs] LockMode 오버로드
 * - [getReferenceAs] 프록시 참조
 * - [createQueryAs] 일반 세션에서 JPQL 실행
 * - [createNativeQueryAs] 네이티브 SQL 실행 (Session / StatelessSession)
 * - [getAs] StatelessSession LockMode 오버로드
 */
@Execution(ExecutionMode.SAME_THREAD)
class MutinySessionSupportTest: AbstractMutinyTest() {

    companion object: KLoggingChannel()

    private val author1 = Author(faker.name().name())
    private val book1 = Book(
        faker.numerify("#-#####-###-#"),
        faker.book().title(),
        LocalDate.of(2003, Month.MARCH, 1)
    )

    @BeforeAll
    fun beforeAll() = runSuspendIO {
        author1.addBook(book1)
        sf.withTransactionSuspending { session ->
            session.persistAll(author1).awaitSuspending()
        }
    }

    /**
     * [findAs] LockMode 오버로드로 엔티티를 조회합니다.
     * LockModeType(JPA) 과 달리 Hibernate 자체 [LockMode] 를 사용합니다.
     */
    @Test
    fun `findAs LockMode 오버로드로 엔티티를 조회한다`() = runSuspendIO {
        val book = sf.withSessionSuspending { session ->
            session.findAs<Book>(book1.id, LockMode.NONE).awaitSuspending()
        }
        book.shouldNotBeNull()
        book.id shouldBeEqualTo book1.id
        book.title shouldBeEqualTo book1.title
    }

    /**
     * [getReferenceAs] 로 Hibernate 프록시 참조를 가져옵니다.
     * 프록시의 id 는 DB 접근 없이 반환됩니다.
     */
    @Test
    fun `getReferenceAs 로 엔티티 프록시 참조를 가져온다`() = runSuspendIO {
        sf.withSessionSuspending { session ->
            val ref = session.getReferenceAs<Book>(book1.id)
            ref.shouldNotBeNull()
            ref.id shouldBeEqualTo book1.id
        }
    }

    /**
     * [createQueryAs] 를 일반 세션(non-stateless)에서 사용해 JPQL 쿼리를 실행합니다.
     */
    @Test
    fun `session 에서 createQueryAs 로 JPQL 쿼리를 실행한다`() = runSuspendIO {
        val books = sf.withSessionSuspending { session ->
            session.createQueryAs<Book>("select b from Book b")
                .resultList
                .awaitSuspending()
        }
        books.size shouldBeGreaterOrEqualTo 1
    }

    /**
     * [createNativeQueryAs] 로 일반 세션에서 네이티브 SQL 쿼리를 실행합니다.
     */
    @Test
    fun `session 에서 createNativeQueryAs 로 네이티브 SQL 를 실행한다`() = runSuspendIO {
        val count = sf.withSessionSuspending { session ->
            session.createNativeQueryAs<Long>("SELECT COUNT(*) FROM books")
                .singleResult
                .awaitSuspending()
        }
        count shouldBeGreaterOrEqualTo 1L
    }

    /**
     * [getAs] StatelessSession LockMode 오버로드로 엔티티를 조회합니다.
     */
    @Test
    fun `statelessSession 에서 getAs LockMode 오버로드로 엔티티를 조회한다`() = runSuspendIO {
        val author = sf.withStatelessSessionSuspending { session ->
            session.getAs<Author>(author1.id, LockMode.NONE).awaitSuspending()
        }
        author.shouldNotBeNull()
        author.id shouldBeEqualTo author1.id
        author.name shouldBeEqualTo author1.name
    }

    /**
     * [createNativeQueryAs] 를 StatelessSession 에서 사용해 네이티브 SQL 쿼리를 실행합니다.
     */
    @Test
    fun `statelessSession 에서 createNativeQueryAs 로 네이티브 SQL 을 실행한다`() = runSuspendIO {
        val count = sf.withStatelessSessionSuspending { session ->
            session.createNativeQueryAs<Long>("SELECT COUNT(*) FROM authors")
                .singleResult
                .awaitSuspending()
        }
        count shouldBeGreaterOrEqualTo 1L
    }
}
