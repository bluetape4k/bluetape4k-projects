package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher

/**
 * 도메인 객체의 변경 이벤트를 표준 출력(Console)으로 출력하는 [JaversDispatcher] 구현체.
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
