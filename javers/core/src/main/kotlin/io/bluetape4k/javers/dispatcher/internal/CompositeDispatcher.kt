package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.collections.forEachCatching
import io.bluetape4k.javers.dispatcher.JaversDispatcher


open class CompositeDispatcher(
    val dispatchers: Collection<JaversDispatcher>,
): JaversDispatcher {

    override fun sendSaved(domainObject: Any) {
        dispatchers.forEachCatching { dispatcher ->
            dispatcher.sendSaved(domainObject)
        }
    }

    override fun sendDeleted(domainObject: Any) {
        dispatchers.forEachCatching { dispatcher ->
            dispatcher.sendDeleted(domainObject)
        }
    }

    override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
        dispatchers.forEachCatching { dispatcher ->
            dispatcher.sendDeletedById(domainObjectId, domainType)
        }
    }
}
