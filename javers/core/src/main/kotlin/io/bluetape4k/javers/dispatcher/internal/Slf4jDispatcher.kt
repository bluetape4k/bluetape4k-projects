package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher
import io.bluetape4k.logging.info

/**
 * 도메인 객체의 변경 이벤트를 SLF4J [org.slf4j.Logger]로 출력하는 [JaversDispatcher] 구현체.
 *
 * @property logger 이벤트를 기록할 SLF4J 로거
 */
class Slf4jDispatcher(private val logger: org.slf4j.Logger): JaversDispatcher {

    override fun sendSaved(domainObject: Any) {
        logger.info { "Send saved domain object. $domainObject" }
    }

    override fun sendDeleted(domainObject: Any) {
        logger.info { "Send deleted domain object. $domainObject" }
    }

    override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
        logger.info { "Send deleted domain object by id. id=$domainObjectId, type=$domainType" }
    }
}
