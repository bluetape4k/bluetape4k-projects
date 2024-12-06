package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 디버그를 위해 Dispatcher에 호출되는 Domain Object를 보관하도록 합니다.
 *
 * @param dispatchers 실제로 외부로 event 를 보내는 [JaversDispatcher]의 컬렉션
 */
class DebugDispacher(dispatchers: Collection<JaversDispatcher>): CompositeDispatcher(dispatchers) {

    data class DeletedById(val id: Any, val domainType: Class<*>)

    private val savedObjects = CopyOnWriteArrayList<Any>()
    private val deletedObjects = CopyOnWriteArrayList<Any>()
    private val deletedByIds = CopyOnWriteArrayList<DeletedById>()

    override fun sendSaved(domainObject: Any) {
        savedObjects.add(domainObject)
        super.sendSaved(domainObject)
    }

    override fun sendDeleted(domainObject: Any) {
        deletedObjects.add(domainObject)
        super.sendDeleted(domainObject)
    }

    override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
        deletedByIds.add(DeletedById(domainObjectId, domainType))
        super.sendDeletedById(domainObjectId, domainType)
    }

    fun isSaved(domainObject: Any): Boolean = savedObjects.contains(domainObject)
    fun isDeleted(domainObject: Any): Boolean = deletedObjects.contains(domainObject)
    fun isDeletedById(id: Any, domainType: Class<*>): Boolean = deletedByIds.contains(DeletedById(id, domainType))

    fun clear() {
        savedObjects.clear()
        deletedObjects.clear()
        deletedByIds.clear()
    }
}
