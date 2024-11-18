package io.bluetape4k.hibernate.reactive.examples.mutiny

import io.bluetape4k.hibernate.reactive.AbstractHibernateReactiveTest
import io.bluetape4k.hibernate.reactive.mutiny.asMutinySessionFactory
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.closeSafe
import org.junit.jupiter.api.AfterAll

abstract class AbstractMutinyTest: AbstractHibernateReactiveTest() {

    companion object: KLogging()

    protected val sf by lazy { getEntityManagerFacotry().asMutinySessionFactory() }

    @AfterAll
    open fun afterAll() {
        if (sf.isOpen) {
            log.debug { "Close Mutiny.SessionFactory" }
            sf.closeSafe()
            Thread.sleep(10)
        }
        log.debug { "Cleanup" }
    }
}
