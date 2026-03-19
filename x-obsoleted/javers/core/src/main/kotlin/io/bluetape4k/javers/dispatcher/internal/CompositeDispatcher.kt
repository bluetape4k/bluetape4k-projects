package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.collections.forEachCatching
import io.bluetape4k.javers.dispatcher.JaversDispatcher


/**
 * 여러 [JaversDispatcher]에 이벤트를 순차적으로 전파하는 복합 디스패처.
 *
 * ## 동작/계약
 * - 등록된 모든 디스패처에 이벤트를 전달하며, 개별 디스패처 예외는 무시하고 나머지를 계속 실행한다
 *
 * @property dispatchers 이벤트를 전달할 [JaversDispatcher] 컬렉션
 */
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
