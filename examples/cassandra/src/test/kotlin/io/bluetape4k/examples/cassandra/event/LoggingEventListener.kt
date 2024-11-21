package io.bluetape4k.examples.cassandra.event

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.springframework.data.cassandra.core.mapping.event.AbstractCassandraEventListener
import org.springframework.data.cassandra.core.mapping.event.AfterConvertEvent
import org.springframework.data.cassandra.core.mapping.event.AfterDeleteEvent
import org.springframework.data.cassandra.core.mapping.event.AfterLoadEvent
import org.springframework.data.cassandra.core.mapping.event.AfterSaveEvent
import org.springframework.data.cassandra.core.mapping.event.BeforeDeleteEvent
import org.springframework.data.cassandra.core.mapping.event.BeforeSaveEvent

class LoggingEventListener: AbstractCassandraEventListener<Any>() {

    companion object: KLogging()

    override fun onBeforeSave(event: BeforeSaveEvent<Any>) {
        log.info { "onBeforeSave: ${event.source}, ${event.statement}" }
    }

    override fun onAfterSave(event: AfterSaveEvent<Any>) {
        log.info { "onAfterSave: ${event.source}" }
    }

    override fun onBeforeDelete(event: BeforeDeleteEvent<Any>) {
        log.info { "onBeforeDelete: ${event.source}" }
    }

    override fun onAfterDelete(event: AfterDeleteEvent<Any>) {
        log.info { "onAfterDelete: ${event.source}" }
    }

    override fun onAfterLoad(event: AfterLoadEvent<Any>) {
        log.info { "onAfterLoad: ${event.source}" }
    }

    override fun onAfterConvert(event: AfterConvertEvent<Any>) {
        log.info { "onAfterConvert: ${event.source}" }
    }
}
