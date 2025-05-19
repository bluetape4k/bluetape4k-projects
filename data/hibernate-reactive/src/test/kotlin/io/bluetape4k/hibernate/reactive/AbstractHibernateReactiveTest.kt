package io.bluetape4k.hibernate.reactive

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence

abstract class AbstractHibernateReactiveTest {

    companion object: KLoggingChannel() {
        val faker = Fakers.faker
    }

    protected fun getEntityManagerFacotry(): EntityManagerFactory {
        val props = MySQLLauncher.hibernateProperties
        return Persistence.createEntityManagerFactory("default", props)
    }
}
