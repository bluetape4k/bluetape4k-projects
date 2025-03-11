package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher

/**
 * Domain Object의 변화를 Console로 출력하는 [JaversDispatcher] 입니다.
 *
 * @property logger slf4j logger
 */
class ConsoleDispatcher: JaversDispatcher {

    override fun sendSaved(domainObject: Any) {
        println("Send saved domain object. $domainObject")
    }

    override fun sendDeleted(domainObject: Any) {
        println("Send deleted domain object. $domainObject")
    }

    override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
        println("Send deleted domain object by id. id=$domainObjectId, type=$domainType")
    }
}
