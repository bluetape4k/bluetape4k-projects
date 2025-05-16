package io.bluetape4k.hibernate.reactive.examples.stage

import io.bluetape4k.hibernate.reactive.AbstractHibernateReactiveTest
import io.bluetape4k.hibernate.reactive.stage.asStageSessionFactory
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.closeSafe
import org.junit.jupiter.api.AfterAll

abstract class AbstractStageTest: AbstractHibernateReactiveTest() {

    companion object: KLoggingChannel()

    protected val sf by lazy { getEntityManagerFacotry().asStageSessionFactory() }

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
