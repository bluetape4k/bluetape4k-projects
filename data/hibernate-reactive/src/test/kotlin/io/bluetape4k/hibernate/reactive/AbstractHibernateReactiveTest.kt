package io.bluetape4k.hibernate.reactive

import io.bluetape4k.hibernate.reactive.examples.model.Author
import io.bluetape4k.hibernate.reactive.examples.model.Book
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence
import java.time.LocalDate

abstract class AbstractHibernateReactiveTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val faker = Fakers.faker

        @JvmStatic
        protected fun newAuthor(): Author =
            Author(name = faker.name().name())

        @JvmStatic
        protected fun newBook(published: LocalDate): Book =
            Book(
                isbn = faker.numerify("#-#####-###-#"),
                title = faker.book().title(),
                published = published
            )
    }

    protected fun getEntityManagerFacotry(): EntityManagerFactory {
        val props = MySQLLauncher.hibernateProperties
        return Persistence.createEntityManagerFactory("default", props)
    }

    protected fun Author.logging() {
        log.debug { "author=$this" }
        this.books.forEach { b ->
            log.debug { "    book=$b" }
        }
    }
}
