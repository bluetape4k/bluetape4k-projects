package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 디스패치된 도메인 객체를 내부에 보관하여 검증할 수 있는 디버그용 [CompositeDispatcher].
 *
 * ## 동작/계약
 * - 전달된 모든 이벤트 객체를 [CopyOnWriteArrayList]에 보관한다
 * - [isSaved], [isDeleted], [isDeletedById]로 이벤트 수신 여부를 검증한다
 * - [clear]로 보관된 이벤트를 모두 초기화한다
 *
 * @param dispatchers 실제로 외부로 이벤트를 전파하는 [JaversDispatcher] 컬렉션
 */
class DebugDispacher(dispatchers: Collection<JaversDispatcher>): CompositeDispatcher(dispatchers) {

    /**
     * ID 기반 삭제 이벤트 정보를 담는 데이터 클래스.
     */
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

    /** 지정한 도메인 객체에 대해 저장 이벤트가 발생했는지 확인한다. */
    fun isSaved(domainObject: Any): Boolean = savedObjects.contains(domainObject)

    /** 지정한 도메인 객체에 대해 삭제 이벤트가 발생했는지 확인한다. */
    fun isDeleted(domainObject: Any): Boolean = deletedObjects.contains(domainObject)

    /** 지정한 ID와 타입으로 삭제 이벤트가 발생했는지 확인한다. */
    fun isDeletedById(id: Any, domainType: Class<*>): Boolean = deletedByIds.contains(DeletedById(id, domainType))

    /** 보관된 모든 이벤트 기록을 초기화한다. */
    fun clear() {
        savedObjects.clear()
        deletedObjects.clear()
        deletedByIds.clear()
    }
}
